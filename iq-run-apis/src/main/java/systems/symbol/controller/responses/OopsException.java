package systems.symbol.controller.responses;

import jakarta.ws.rs.core.Response;

public class OopsException extends Exception {
    String msg;
    Response.Status status;

    public OopsException(String msg, Response.Status status) {
        this.msg = msg;
        this.status = status;
    }

    public String getMessage() {
        return this.msg;
    }

    public Response.Status getStatus() {
        return this.status;
    }
}
