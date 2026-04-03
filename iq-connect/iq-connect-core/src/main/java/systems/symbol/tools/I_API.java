package systems.symbol.tools;

import java.io.IOException;
import java.util.Map;

public interface I_API<T> {

String getURL();

T head(Map<String, Object> queryParams) throws IOException, APIException;

T get() throws IOException, APIException;

T get(Map<String, Object> queryParams) throws IOException, APIException;

T delete(Map<String, Object> queryParams) throws IOException, APIException;

T post(Map<String, Object> json) throws IOException, APIException;

T put(Map<String, Object> json) throws IOException, APIException;
}
