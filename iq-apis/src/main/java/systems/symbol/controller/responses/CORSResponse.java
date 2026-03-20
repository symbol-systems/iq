package systems.symbol.controller.responses;

import jakarta.ws.rs.core.Response;

public class CORSResponse implements I_Response {

    public CORSResponse() {
    }

    public Response build() {
        return addCORS(Response.ok()).build();
    }
}
