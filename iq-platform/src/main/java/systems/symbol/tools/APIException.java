package systems.symbol.tools;

import okhttp3.Response;

public class APIException extends Throwable {
String baseURL, message;
Response response;

public APIException(String message, String baseURL, Response response) {
this.message = message;
this.baseURL = baseURL;
this.response = response;
}
}
