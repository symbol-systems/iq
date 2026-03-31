package systems.symbol.mcp;

import jakarta.ws.rs.core.Response;

/**
 * MCPAuthException — authentication-specific MCP exception.
 *
 * <p>Raised when JWT parsing, token validation, or credential extraction fails.
 * Carries an HTTP status code for appropriate REST response mapping.
 *
 * <p>Typical codes:
 * <ul>
 *   <li>401 — unauthorized (invalid or expired token)</li>
 *   <li>403 — forbidden (valid token but principal lacks realm access)</li>
 * </ul>
 */
public class MCPAuthException extends MCPException {

    private final Response.Status httpStatus;

    /**
     * Construct an auth exception with HTTP status mapping.
     *
     * @param message error description
     * @param httpStatus HTTP status to return to client
     */
    public MCPAuthException(String message, Response.Status httpStatus) {
        super(httpStatus.getStatusCode(), message);
        this.httpStatus = httpStatus;
    }

    /**
     * Construct an auth exception with HTTP status and root cause.
     *
     * @param message error description
     * @param httpStatus HTTP status to return to client
     * @param cause underlying exception
     */
    public MCPAuthException(String message, Response.Status httpStatus, Throwable cause) {
        super(httpStatus.getStatusCode(), message, cause);
        this.httpStatus = httpStatus;
    }

    /**
     * Get the HTTP status code for REST error response.
     */
    public Response.Status getHttpStatus() {
        return httpStatus;
    }

    /* ── convenience factories ─────────────────────────────────────────── */

    public static MCPAuthException unauthorized(String msg) {
        return new MCPAuthException(msg, Response.Status.UNAUTHORIZED);
    }

    public static MCPAuthException forbidden(String msg) {
        return new MCPAuthException(msg, Response.Status.FORBIDDEN);
    }

    public static MCPAuthException unauthorized(String msg, Throwable cause) {
        return new MCPAuthException(msg, Response.Status.UNAUTHORIZED, cause);
    }

    public static MCPAuthException forbidden(String msg, Throwable cause) {
        return new MCPAuthException(msg, Response.Status.FORBIDDEN, cause);
    }
}
