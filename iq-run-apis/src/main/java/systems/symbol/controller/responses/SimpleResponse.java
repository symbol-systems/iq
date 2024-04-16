package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleResponse implements I_Response {
public Object data;
public Map<String, Object> meta = new HashMap<>();
public SimpleResponse(Object data) {
this.data = data;
if (data instanceof List) {
this.meta.put("count", ((List<?>)data).size());
}
this.meta.put("created", System.currentTimeMillis());
}

public Response asJSON() {
Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this);
return I_Response.addCORS(builder).build();
}

//public Response asJSON() {
//return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this).build();
//}
}
