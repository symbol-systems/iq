package systems.symbol.tools;

import com.amazonaws.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import okhttp3.FormBody.Builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the I_API interface using OkHttp for REST API calls.
 */
public class RestAPI implements I_API<Response> {
private static final Logger log = LoggerFactory.getLogger(RestAPI.class);
private final OkHttpClient client;
private final ObjectMapper objectMapper = new ObjectMapper();
private final String baseURL;
private final HttpUrl httpUrl;
private final Map<String, String> headers = new HashMap<>();
private final I_Secrets secrets;

/**
 * Creates a new instance of the RestAPI class.
 *
 * @param baseURL The base URL of the API.
 */
public RestAPI(String baseURL) {
this(baseURL, null);
}

/**
 * Creates a new instance of the RestAPI class.
 *
 * @param baseURL The base URL of the API.
 */
public RestAPI(String baseURL, I_Secrets secrets) {
OkHttpClient.Builder builder = new OkHttpClient.Builder();
builder.addInterceptor(new LoggingInterceptor());
builder.connectTimeout(Duration.ofSeconds(30));
builder.callTimeout(Duration.ofSeconds(30));
this.client = builder.build();
this.baseURL = baseURL;
this.httpUrl = HttpUrl.parse(getURL());
this.secrets = secrets;
}

/**
 * Gets the base URL of the API.
 *
 * @return The base URL.
 */
@Override
public String getURL() {
return baseURL;
}

public RestAPI bearer() throws SecretsException {
if (this.secrets == null)
throw new SecretsException("missing.bearer.secrets");
String authToken = this.secrets.getSecret(getURL());
if (authToken == null)
throw new SecretsException("missing.api.secret");
return header("Authorization", "Bearer " + authToken);
}

public RestAPI bearer(String authToken) throws SecretsException {
return header("Authorization", "Bearer " + authToken);
}

public RestAPI authorization(String prefix) throws SecretsException {
if (this.secrets == null)
throw new SecretsException("missing.auth.secrets");
String authToken = this.secrets.getSecret(getURL());
if (Validate.isMissing(authToken))
throw new SecretsException("missing.auth.secret");
return header("Authorization", prefix + authToken);
}

public RestAPI authorization() throws SecretsException {
if (this.secrets == null)
throw new SecretsException("missing.auth.secrets");
String authToken = this.secrets.getSecret(getURL());
if (Validate.isMissing(authToken))
throw new SecretsException("missing.auth.secret");
return header("Authorization", authToken);
}

public RestAPI basic() throws SecretsException {
if (this.secrets == null)
throw new SecretsException("missing.basic.secrets");
String credentials = this.secrets.getSecret(getURL());
String encoded = Base64.encodeAsString(credentials.getBytes(StandardCharsets.UTF_8));
log.info("ux.basic.auth: {} -> {}", credentials, encoded);
header("Authorization", "Basic " + encoded);
return this;
}

public RestAPI header(String name) throws SecretsException {
if (this.secrets == null)
throw new SecretsException("missing.header.secrets");
String credentials = this.secrets.getSecret(getURL());
log.info("ux.header.auth: {} -> {}", name, credentials);
return header(name, credentials);
}

public RestAPI header(String name, String value) {
if (value != null)
this.headers.put(name, value);
return this;
}

/**
 * Creates a new map for query parameters.
 *
 * @return A new map for query parameters.
 */
public static Map<String, Object> newParams() {
return new HashMap<>();
}

/**
 * Makes a HEAD request to the API with optional query parameters.
 *
 * @param queryParams The query parameters.
 * @return The response body.
 * @throws IOException  If an I/O error occurs.
 * @throws APIException If the API response is not successful.
 */
@Override
public Response head(Map<String, Object> queryParams) throws IOException, APIException {
HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
buildUrlWithQueryParameters(urlBuilder, queryParams);

Request request = createRequestBuilder().url(urlBuilder.build()).head().build();
return executeRequest(request);
}

/**
 * Makes a GET request to the API with optional query parameters.
 *
 * @return The response body.
 * @throws IOException  If an I/O error occurs.
 * @throws APIException If the API response is not successful.
 */
@Override
public Response get() throws IOException, APIException {
return get(null);
}

/**
 * Makes a GET request to the API with optional query parameters.
 *
 * @param queryParams The query parameters.
 * @return The response body.
 * @throws IOException  If an I/O error occurs.
 * @throws APIException If the API response is not successful.
 */
@Override
public Response get(Map<String, Object> queryParams) throws IOException, APIException {
HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
if (queryParams != null)
buildUrlWithQueryParameters(urlBuilder, queryParams);

Request request = createRequestBuilder().url(urlBuilder.build()).get().build();
return executeRequest(request);
}

/**
 * Makes a DELETE request to the API with optional query parameters.
 *
 * @param queryParams The query parameters.
 * @return The response body.
 * @throws IOException  If an I/O error occurs.
 * @throws APIException If the API response is not successful.
 */
@Override
public Response delete(Map<String, Object> queryParams) throws IOException, APIException {
HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
buildUrlWithQueryParameters(urlBuilder, queryParams);

Request request = createRequestBuilder().url(urlBuilder.build()).delete().build();
return executeRequest(request);
}

public FormBody.Builder form(Map<String, Object> queryParams) {
FormBody.Builder formBody = new FormBody.Builder();
for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
formBody.add(entry.getKey(), String.valueOf(entry.getValue()));
}
return formBody;
}

public Response multipart(InputStream in, String filename, Map<String, Object> queryParams)
throws IOException {
okhttp3.MultipartBody.Builder multipart = MultipartRequestBuilder.multipart(in, filename, queryParams);
Request.Builder builder = createRequestBuilder().url(getURL());
MultipartBody multipartBody = multipart.build();

Request req = builder.post(multipartBody).build();
return executeRequest(req);
}

/**
 * Makes a POST request to the API with a JSON body.
 *
 * @param json The JSON body.
 * @return The response body.
 * @throws IOException  If an I/O error occurs.
 * @throws APIException If the API response is not successful.
 */
@Override
public Response post(Map<String, Object> json) throws IOException, APIException {
String jsonBody = convertObjectToJsonString(json);
String contentType = headers.get("Content-Type");
if (contentType == null || contentType.isEmpty())
contentType = "application/json";
if (contentType.equalsIgnoreCase("application/x-www-form-urlencoded")) {
log.info("ux.form: {} -> {}", json.keySet(), getURL());
// MyFacade.dump(json, System.out);
Request.Builder builder = createRequestBuilder().url(getURL());
FormBody formBody = form(json).build();
builder.post(formBody);
return executeRequest(builder.build());
} else {
RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse(contentType));
log.debug("ux.post: {} -> {} / {}", getURL(), requestBody.contentType(), requestBody.contentLength());
// MyFacade.dump(json, System.out);
Request.Builder builder = createRequestBuilder().url(getURL());
builder.post(requestBody);
return executeRequest(builder.build());
}
}

/**
 * Makes a PUT request to the API with a JSON body.
 *
 * @param json The JSON body.
 * @return The response body.
 * @throws IOException  If an I/O error occurs.
 * @throws APIException If the API response is not successful.
 */
@Override
public Response put(Map<String, Object> json) throws IOException, APIException {
String jsonBody = convertObjectToJsonString(json);
RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));

Request request = createRequestBuilder().url(getURL()).put(requestBody).build();
return executeRequest(request);
}

/**
 * Builds the URL with optional query parameters.
 *
 * @param urlBuilder  The URL builder.
 * @param queryParams The query parameters.
 */
private void buildUrlWithQueryParameters(HttpUrl.Builder urlBuilder, Map<String, Object> queryParams) {
if (queryParams != null) {
for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
urlBuilder.addQueryParameter(entry.getKey(), entry.getValue().toString());
}
}
}

/**
 * Creates a request builder with common headers.
 *
 * @return The request builder.
 */
private Request.Builder createRequestBuilder() {
Request.Builder builder = new Request.Builder();
for (String n : headers.keySet()) {
builder.header(n, headers.get(n));
}
log.debug("ux.headers: {} -> {}", getURL(), headers);
return builder;
}

/**
 * Executes the HTTP request and returns the response.
 *
 * @param request The HTTP request.
 * @return The response body.
 * @throws IOException If an I/O error occurs.
 */
private Response executeRequest(Request request) throws IOException {
try {
log.debug("ux.request: {} -> {}", request.url(), request.headers().toMultimap().keySet());
return client.newCall(request).execute();
} catch (java.net.SocketTimeoutException e) {
// retry
log.warn("ux.retry: {}", request);
return client.newCall(request).execute();
}
}

/**
 * Converts an object to a JSON string using Jackson ObjectMapper.
 *
 * @param object The object to convert.
 * @return The JSON string representation of the object.
 * @throws IOException If an I/O error occurs during serialization.
 */
private String convertObjectToJsonString(Object object) throws IOException {
return objectMapper.writeValueAsString(object);
}
}

class LoggingInterceptor implements Interceptor {
@Override
public Response intercept(Chain chain) throws IOException {
Request request = chain.request();
// Clone the request to log its body
// Request copy = request.newBuilder().build();
// if (copy.body() != null) {
// Buffer buffer = new Buffer();
// copy.body().writeTo(buffer);
// String requestBodyString = buffer.readUtf8();
//// System.out.println("ux.debug: " + requestBodyString);
// }

return chain.proceed(request);
}
}
