package systems.symbol.mcp;

/**
 * I_MCPResource — MCP resource provider SPI.
 *
 * Resources are read-only, ambient context that clients pull into their
 * context window (e.g., schema, namespaces, access policies).
 * They differ from tools in being non-mutating and not requiring middleware guards.
 */
public interface I_MCPResource {

    /**
     * Base or template URI for this resource.
     * Template URIs may contain path variables like {@code {realm}}.
     */
    String getUri();

    /**
     * Human-readable label shown in the MCP manifest.
     */
    String getLabel();

    /**
     * Prose description of the resource's content and usage.
     */
    String getDescription();

    /**
     * MIME type of the resource content (e.g., {@code text/turtle}, {@code application/ld+json}).
     */
    String getMimeType();

    /**
     * Returns {@code true} if this provider serves the given concrete URI.
     * Default implementation performs exact URI match.
     */
    default boolean matchesUri(String uri) {
        return getUri().equals(uri);
    }

    /**
     * Read and return the resource content.
     *
     * @param ctx   the call context (principal, realm)
     * @param uri   the concrete URI requested by the client
     * @return      non-null result carrying the serialized content
     * @throws MCPException on read or authorization failure
     */
    I_MCPResult read(MCPCallContext ctx, String uri) throws MCPException;
}
