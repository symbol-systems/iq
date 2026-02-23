package systems.symbol.tools;

import okhttp3.*;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.SecretsException;

import java.io.IOException;
import java.util.Map;

public class MockAPI implements I_API<Response> {
    public String url, secret, body;

    public MockAPI(String url, String secret, String body) {
        this.url = url;
        this.secret = secret;
        this.body = body;
    }

    static public Response.Builder build(String url, String secret, int statusCode, String content) {
        ResponseBody body = ResponseBody.create(content, MediaType.get("application/json"));

        Response.Builder builder = new Response.Builder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .protocol(Protocol.HTTP_1_1)
                .message("ok")
                .body(body)
                .request(new Request.Builder().url(url).build());
        if (secret != null)
            builder.addHeader("Authorization", "Bearer " + secret);

        builder.setCode$okhttp(statusCode);
        return builder;
    }

    public MockAPI(APISecrets ignored, String url, String secret) throws SecretsException {
        this.url = url;
        this.secret = secret;
    }

    @Override
    public Response get(Map<String, Object> queryParams) {
        return build(url, secret, 200, body).build();
    }

    @Override
    public Response post(Map<String, Object> json) {
        return build(url, secret, 201, body).build();
    }

    @Override
    public Response delete(Map<String, Object> queryParams) {
        return build(url, secret, 200, body).build();

    }

    @Override
    public Response put(Map<String, Object> json) {
        return build(url, secret, 200, body).build();

    }

    @Override
    public Response head(Map<String, Object> queryParams) {
        return build(url, secret, 200, "").build();
    }

    @Override
    public Response get() throws IOException, APIException {
        return get(null);
    }

    @Override
    public String getURL() {
        return url;
    }

    public String getAuthToken() {
        return this.secret;
    }

    public String toString() {
        return url + " @ " + secret;
    }
}
