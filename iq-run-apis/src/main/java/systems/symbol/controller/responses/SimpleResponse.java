package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;

public class SimpleResponse implements I_Response {
    public Object data;

    public SimpleResponse() {
    }

    public SimpleResponse(Object data) {
        this.data = data;
    }

    public SimpleResponse(String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        this.data = data;
    }

    public Response asJSON() {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(data);
        return addCORS(builder).build();
    }
}
