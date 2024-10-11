package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

public class OopsResponse implements I_Response {
public Object message;
public Map<String, Object> meta = new HashMap<>();
private final int status;

public OopsResponse(String message, Response.Status status) {
this.status = status.getStatusCode();
this.message = message;
this.meta.put("created", System.currentTimeMillis());
}

public OopsResponse(String message, Exception e) {
this.message = message;
this.status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
this.meta.put("error", e.getMessage());
}

public OopsResponse(String message, int status) {
this.message = message;
this.status = status;
}

public Response build() {
Response.ResponseBuilder builder = Response.status(this.status).type(MediaType.APPLICATION_JSON_TYPE)
.entity(this);
return addCORS(builder).build();
}
}
