package systems.symbol.mcp;

/**
 * I_MCPResult — Immutable result envelope returned by tools, resources, and prompts.
 *
 * <p>Keeps the adapter layer decoupled from the MCP SDK types.
 * The server wiring layer ({@link systems.symbol.mcp.server.MCPServer}) translates
 * {@code I_MCPResult} into the appropriate SDK DTO
 * ({@code McpSchema.CallToolResult}, {@code McpSchema.ReadResourceResult}, etc.).
 *
 * <p><b>Content negotiation</b> — the MIME type signals how the server should
 * encode the payload; common values: {@code application/json},
 * {@code application/ld+json}, {@code text/turtle}, {@code text/plain}.
 *
 * <p><b>Versioning note</b> — a future reactive variant will add a
 * {@code Publisher<I_MCPResult> stream()} method for streaming large results.
 */
public interface I_MCPResult {

    /** Whether this result represents an error condition. */
    boolean isError();

    /**
     * The primary payload.  For errors this is a JSON-encoded error object;
     * for success results it is the serialised content.
     */
    String getContent();

    /** MIME type of {@link #getContent()}. */
    String getMimeType();

    /**
     * Optional error code (mirrors HTTP conventions; {@code 0} = no error).
     * Examples: {@code 403} permission denied, {@code 429} quota exceeded,
     * {@code 500} internal error.
     */
    default int getErrorCode() { return 0; }

    /** Convenience: success result carrying the given content. */
    static I_MCPResult ok(String content, String mimeType) {
        return MCPResult.ok(content, mimeType);
    }

    /** Convenience: success result with JSON content. */
    static I_MCPResult okJson(String json) {
        return MCPResult.ok(json, "application/json");
    }

    /** Convenience: error result. */
    static I_MCPResult error(int code, String message) {
        return MCPResult.error(code, message);
    }
}
