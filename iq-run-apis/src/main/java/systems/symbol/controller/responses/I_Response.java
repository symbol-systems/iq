package systems.symbol.controller.responses;

import jakarta.ws.rs.core.Response;
import systems.symbol.string.PrettyString;

public interface I_Response {
    public Response build();

    default Response.ResponseBuilder addCORS(Response.ResponseBuilder builder) {
        return builder.header("Access-Control-Allow-Origin", PrettyString.getenv("MY_CORS_ORIGIN", "*"))
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
                .header("Access-Control-Allow-Headers",
                        PrettyString.getenv("MY_CORS_HEADERS", "origin, content-type, accept, authorization"));
    }

}
