package systems.symbol.tools;

import okhttp3.Response;

public class APIException extends Exception {
    String baseURL;
    Response response;

    public APIException(String message, String baseURL, Response response) {
        super(message);
        this.baseURL = baseURL;
        this.response = response;
    }
}
