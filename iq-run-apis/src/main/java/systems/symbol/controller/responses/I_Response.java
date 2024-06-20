package systems.symbol.controller.responses;

import jakarta.ws.rs.core.Response;

public interface I_Response {
    public Response asJSON();

    default Response.ResponseBuilder addCORS(Response.ResponseBuilder builder) {
        return builder.header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
                .header("Access-Control-Allow-Headers",
                        "origin, content-type, accept, authorization");
    }

}
