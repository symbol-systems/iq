package systems.symbol.mcp;

import java.util.Map;

/**
 * I_MCPTool — Service-provider interface for an MCP tool adapter.
 *
 * <p>Every capability exposed to an LLM client as an MCP {@code Tool} must
 * implement this interface.  The concrete tool is responsible only for its
 * execution logic; governance (auth, ACL, quota, audit) is handled by the
 * {@link systems.symbol.mcp.connect.MCPConnectPipeline} upstream.
 *
 * <p><b>Design contract</b>
 * <ul>
 *   <li>Implementations must be <em>stateless and thread-safe</em>.</li>
 *   <li>Input and output are plain {@code Map<String,Object>} (JSON-LD friendly).</li>
 *   <li>Exceptions surface as {@link MCPException} so the pipeline can map them to
 *       MCP error responses consistently.</li>
 * </ul>
 *
 * <p><b>Versioning note</b> — future reactive/streaming variants will extend this
 * interface with a {@code Publisher<I_MCPResult> stream(MCPCallContext, Map)} overload,
 * keeping the synchronous contract stable.
 */
public interface I_MCPTool {

    /**
     * Canonical tool name used in MCP registration.
     * Convention: {@code domain.verb}  e.g. {@code sparql.query}, {@code actor.trigger}.
     */
    String getName();

    /** Human-readable description shown to the LLM in the tool manifest. */
    String getDescription();

    /**
     * JSON Schema describing the tool's input parameters.
     * Returned as a structured {@code Map} so callers can serialise it to any format
     * (Jackson, Gson, string JSON) without a hard import.
     */
    Map<String, Object> getInputSchema();

    /**
     * JSON Schema describing the tool's output/result structure.
     * Default: generic object schema (no constraint).
     * Override to provide detailed output documentation.
     */
    default Map<String, Object> getOutputSchema() {
        return Map.of("type", "object", "description", "Tool result");
    }

    /**
     * Example invocations for documentation and testing.
     * Each example is a Map containing:
     * <ul>
     *   <li>"input" — example input parameters</li>
     *   <li>"output" — example result or explanation</li>
     *   <li>"description" — narrative description of the example</li>
     * </ul>
     * Default: empty list.
     */
    default java.util.List<Map<String, Object>> getExamples() {
        return java.util.List.of();
    }

    /**
     * Execute the tool.
     *
     * @param ctx    the fully-populated call context (principal, realm, metadata)
     * @param input  validated input parameters matching {@link #getInputSchema()}
     * @return       a non-null {@link I_MCPResult}
     * @throws MCPException on any execution or validation failure
     */
    I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException;

    /** Whether this tool modifies state. Read-only tools may bypass certain guards. */
    default boolean isReadOnly() { return false; }

    /**
     * Advertised rate limit (calls per minute).  {@code 0} means unlimited.
     * Middleware may enforce a stricter per-principal limit from RDF config.
     */
    default int defaultRateLimit() { return 0; }

    /**
     * Declaration order used when building the MCP manifest.
     * Lower numbers appear first in the tool list sent to the LLM.
     */
    default int order() { return 100; }
}
