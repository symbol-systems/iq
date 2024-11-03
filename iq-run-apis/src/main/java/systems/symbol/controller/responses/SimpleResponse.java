package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

public class SimpleResponse implements I_Response {
public Object data;
private MediaType type = MediaType.APPLICATION_JSON_TYPE;

public SimpleResponse() {
}

public SimpleResponse(Object data) {
this.data = data;
}

public SimpleResponse(Object data, MediaType type) {
this.data = data;
this.type = type;
}

public SimpleResponse(String key, Object value) {
Map<String, Object> data = new HashMap<>();
data.put(key, value);
this.data = data;
}

public SimpleResponse type(String _type) {
type = MediaType.valueOf(_type);
return this;
}

public Response build() {
Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(type)
.entity(data);
return addCORS(builder).build();
}
}
