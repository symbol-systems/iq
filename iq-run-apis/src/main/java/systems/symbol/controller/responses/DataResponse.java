package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataResponse implements I_Response {
    public Object data;
    public Map<String, Object> meta = new HashMap<>();
    public DataResponse(Object data) {
        this.data = data;
        if (data instanceof List) {
            this.meta.put("count", ((List<?>)data).size());
        }
        this.meta.put("created", System.currentTimeMillis());
    }

    public DataResponse() {
    }

    public void set(String key, Object value) {
        meta.put(key, value);
    }

    public Response asJSON() {
        return asJSON(Response.Status.OK);
    }

    public Response asJSON(Response.Status status) {
        Response.ResponseBuilder builder = Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(this);
        return addCORS(builder).build();
    }

//    public Response asJSON() {
//        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this).build();
//    }
}
