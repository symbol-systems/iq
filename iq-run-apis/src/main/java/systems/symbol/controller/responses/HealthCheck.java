package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.platform.I_Self;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HealthCheck implements I_Response {
    public String status, now, message, version;

    public HealthCheck(String status) {
        this(status, "");
    }
    public HealthCheck(String status, String message) {
        this.status = status;
        this.now = formatDate(new Date());
        this.message=message;
        try {
            this.version = I_Self.version();
        } catch (IOException e) {
            this.version = "?.?.?";
        }
    }
    private String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

    public Response asJSON() {
        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this).build();
    }
}

