package systems.symbol.agent.tools;

import java.io.IOException;
import java.util.Map;

public interface I_API<T> {

    String getURL();
    String getAuthToken(); //
    T head(Map<String, String> queryParams) throws IOException, APIException;
    T get() throws IOException, APIException;
    T get(Map<String, String> queryParams) throws IOException, APIException;
    T delete(Map<String, String> queryParams) throws IOException, APIException;
    T post(Map<String, Object> json) throws IOException, APIException;
    T put(Map<String, String> json) throws IOException, APIException;
}
