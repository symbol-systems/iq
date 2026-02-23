package systems.symbol.controller.responses;

import jakarta.ws.rs.core.Response;

public class OopsException extends Exception {
    String msg;
    Response.Status status;

    public OopsException(String msg, Response.Status status) {
        this.msg = msg;
        this.status = status;
    }

    public OopsException(String msg) {
        this.msg = msg;
        this.status = Response.Status.BAD_REQUEST;
    }

    public String getMessage() {
        return this.msg;
    }

    public Response.Status getStatus() {
        return this.status;
    }
}
