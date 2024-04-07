package systems.symbol.agent.apis;

import systems.symbol.secrets.SecretsException;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Map;

public class MockAPI implements I_API<String> {
    public String url, secret, body;

    public MockAPI(String url, String secret, String body) {
        this.url = url;
        this.secret = secret;
        this.body = body;
    }

    static public Response.Builder build(String url, String secret, int statusCode, String body) {
        Response.Builder builder = new Response.Builder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-cache")
                .protocol(Protocol.HTTP_1_1)
                .message(body)
                .request(new Request.Builder().url(url).build());
        if (secret!=null) builder.addHeader("Authorization", "Bearer "+secret);

        builder.setCode$okhttp(statusCode);
        return builder;
    }

    public MockAPI(APIs ignored, String url, String secret) throws SecretsException {
        this.url = url;
        this.secret = secret;
    }


    @Override
    public String get(Map<String, String> queryParams) {
        return body;
    }

    @Override
    public String post(Map<String, Object> json) {
        return body;
    }

    @Override
    public String delete(Map<String, String> queryParams) {
        return body;
    }

    @Override
    public String put(Map<String, String> json) {
        return body;
    }

    @Override
    public String head(Map<String, String> queryParams) {
        return null;
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
