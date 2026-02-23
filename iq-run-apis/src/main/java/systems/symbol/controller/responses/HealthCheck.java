package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.platform.I_Self;
import java.util.Date;

public class HealthCheck implements I_Response {
    public boolean ok;
    public String now, message, version;

    public HealthCheck(boolean status) {
        this(status, status ? "ok" : "oops");
    }

    public HealthCheck(boolean status, String message) {
        this.ok = status;
        this.now = formatDate(new Date());
        this.message = message;
        try {
            this.version = I_Self.version();
        } catch (Exception e) {
            this.version = e.getLocalizedMessage();
        }
    }

    public Response build() {
        return addCORS(Response.status(Response.Status.OK)
                .type(MediaType.APPLICATION_JSON_TYPE).entity(this)).build();
    }
}
