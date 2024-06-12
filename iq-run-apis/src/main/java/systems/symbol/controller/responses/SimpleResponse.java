package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JWTResponse implements I_Response {
    public Map<String, Object> data = new HashMap<>();

    public JWTResponse() {
    }

    public JWTResponse(Map<String, Object> data) {
        this.data = data;
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }
    public Response asJSON() {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(data);
        return addCORS(builder).build();
    }

//    public Response asJSON() {
//        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this).build();
//    }
}
