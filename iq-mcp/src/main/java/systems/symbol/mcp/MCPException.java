package systems.symbol.mcp;

/**
 * MCPException — typed exception for all MCP-layer failures.
 *
 * <p>Carries an HTTP-style error code so the server layer can construct
 * the appropriate MCP error response without inspecting the message text.
 *
 * <p>Common codes:
 * <ul>
 *   <li>{@code 400} — bad input / invalid SPARQL</li>
 *   <li>{@code 401} — missing or invalid credentials</li>
 *   <li>{@code 403} — access denied by ACL or trust zone</li>
 *   <li>{@code 404} — resource or tool not found</li>
 *   <li>{@code 429} — quota / rate limit exceeded</li>
 *   <li>{@code 500} — internal error</li>
 *   <li>{@code 503} — backend (RDF4J, LLM) unavailable</li>
 * </ul>
 */
public class MCPException extends Exception {

    private final int code;

    public MCPException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MCPException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /** HTTP-style status code. */
    public int getCode() { return code; }

    /* ── convenience factories ─────────────────────────────────────────── */

    public static MCPException badRequest(String msg)        { return new MCPException(400, msg); }
    public static MCPException unauthorized(String msg)      { return new MCPException(401, msg); }
    public static MCPException forbidden(String msg)         { return new MCPException(403, msg); }
    public static MCPException notFound(String msg)          { return new MCPException(404, msg); }
    public static MCPException quotaExceeded(String msg)     { return new MCPException(429, msg); }
    public static MCPException internal(String msg, Throwable c) { return new MCPException(500, msg, c); }
    public static MCPException serviceUnavailable(String msg){ return new MCPException(503, msg); }

    /** Convert to an error {@link I_MCPResult}. */
    public I_MCPResult toResult() { return MCPResult.error(code, getMessage()); }
}
