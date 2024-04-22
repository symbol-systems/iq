package systems.symbol.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the I_API interface using OkHttp for REST API calls.
 */
public class RestAPI implements I_API<Response> {
    private static final String BEARER_AUTH_HEADER = "Authorization";
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String baseURL;
    private final String authToken;
    private final HttpUrl httpUrl;
    private String authHeader;
    private final Map<String,String> headers = new HashMap<>();

    /**
     * Creates a new instance of the RestAPI class.
     *
     * @param baseURL   The base URL of the API.
     * @param authToken The authentication token for API requests.
     */
    public RestAPI(String baseURL, String authToken, String authHeader) {
        this.client = new OkHttpClient();
        this.baseURL = baseURL;
        this.authToken = authToken;
        this.authHeader = authHeader;
        this.httpUrl = HttpUrl.parse(getURL());
    }

    public RestAPI(String baseURL, String authToken) {
        this(baseURL, authToken, BEARER_AUTH_HEADER);
    }

    public RestAPI(String baseURL) {
        this(baseURL, null, null);
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

    public RestAPI authenticate(String header) {
        this.authHeader = header;
        return this;
    }

    public RestAPI header(String name, String value) {
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
     * @throws IOException   If an I/O error occurs.
     * @throws APIException  If the API response is not successful.
     */
    @Override
    public Response head(Map<String, String> queryParams) throws IOException, APIException {
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        buildUrlWithQueryParameters(urlBuilder, queryParams);

        Request request = createRequestBuilder().url(urlBuilder.build()).head().build();
        return executeRequest(request);
    }

    /**
     * Makes a GET request to the API with optional query parameters.
     *
     * @return The response body.
     * @throws IOException   If an I/O error occurs.
     * @throws APIException  If the API response is not successful.
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
     * @throws IOException   If an I/O error occurs.
     * @throws APIException  If the API response is not successful.
     */
    @Override
    public Response get(Map<String, String> queryParams) throws IOException, APIException {
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (queryParams!=null) buildUrlWithQueryParameters(urlBuilder, queryParams);

        Request request = createRequestBuilder().url(urlBuilder.build()).get().build();
        return executeRequest(request);
    }

    /**
     * Makes a DELETE request to the API with optional query parameters.
     *
     * @param queryParams The query parameters.
     * @return The response body.
     * @throws IOException   If an I/O error occurs.
     * @throws APIException  If the API response is not successful.
     */
    @Override
    public Response delete(Map<String, String> queryParams) throws IOException, APIException {
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        buildUrlWithQueryParameters(urlBuilder, queryParams);

        Request request = createRequestBuilder().url(urlBuilder.build()).delete().build();
        return executeRequest(request);
    }

    /**
     * Makes a POST request to the API with a JSON body.
     *
     * @param json The JSON body.
     * @return The response body.
     * @throws IOException   If an I/O error occurs.
     * @throws APIException  If the API response is not successful.
     */
    @Override
    public Response post(Map<String, Object> json) throws IOException, APIException {
        String jsonBody = convertObjectToJsonString(json);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        log.debug("agent.api.post: {} -> {}", getURL(), jsonBody);
        Request request = createRequestBuilder().url(getURL()).post(requestBody).build();
        return executeRequest(request);
    }

    /**
     * Makes a PUT request to the API with a JSON body.
     *
     * @param json The JSON body.
     * @return The response body.
     * @throws IOException   If an I/O error occurs.
     * @throws APIException  If the API response is not successful.
     */
    @Override
    public Response put(Map<String, String> json) throws IOException, APIException {
        String jsonBody = convertObjectToJsonString(json);
        RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = createRequestBuilder().url(getURL()).put(requestBody).build();
        return executeRequest(request);
    }

    /**
     * Builds the URL with optional query parameters.
     *
     * @param urlBuilder   The URL builder.
     * @param queryParams  The query parameters.
     */
    private void buildUrlWithQueryParameters(HttpUrl.Builder urlBuilder, Map<String, String> queryParams) {
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
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
        for(String n: headers.keySet()) {
            builder.header(n, headers.get(n));
        }
        if (null!= authToken && null != authHeader && !authToken.isEmpty()) {
            if (authHeader.equalsIgnoreCase(BEARER_AUTH_HEADER)) {
                builder.header(authHeader, "Bearer " + authToken);
            } else {
                builder.header(authHeader, authToken);
            }
            log.info("api.bearer: {} -> {}", authHeader, authToken);
        }

        return builder;
    }

    /**
     * Executes the HTTP request and returns the response.
     *
     * @param request The HTTP request.
     * @return The response body.
     * @throws IOException   If an I/O error occurs.
     */
    private Response executeRequest(Request request) throws IOException {
        try {
            return client.newCall(request).execute();
        } catch (java.net.SocketTimeoutException e) {
            // retry
            log.debug("retry: {}", request );
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
