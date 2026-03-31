package systems.symbol.mcp.tool;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SparqlUpdateAdapter — Pillar B: the universal write tool.
 *
 * <p>Accepts a SPARQL Update string and forwards it to the RDF4J repository.
 * This tool requires authentication (enforced by {@code ACLFilterMiddleware}).
 *
 * <p>Safety checks applied before execution:
 * <ol>
 *   <li>Must start with a write keyword — rejects attempts to use this tool as a read path.</li>
 *   <li>DROP GRAPH operations require an explicit {@code confirmDrop=true} flag.</li>
 * </ol>
 *
 * <p>Tool name: {@value systems.symbol.mcp.MCP_NS#TOOL_SPARQL_UPDATE}
 */
public class SparqlUpdateAdapter implements I_MCPTool {

    private static final Logger log = LoggerFactory.getLogger(SparqlUpdateAdapter.class);

    private static final Pattern UPDATE_PATTERN = Pattern.compile(
            "(?i)^\\s*(INSERT|DELETE|DROP|CREATE|LOAD|CLEAR|COPY|MOVE|ADD)");
    private static final Pattern DROP_PATTERN = Pattern.compile("(?i)\\bDROP\\s+GRAPH\\b");

    private final Repository repository;

    public SparqlUpdateAdapter(Repository repository) {
        this.repository = repository;
    }

    @Override public String getName()       { return "sparql.update"; }
    @Override public boolean isReadOnly()   { return false; }
    @Override public int defaultRateLimit() { return 10; }
    @Override public int order()            { return 20; }

    @Override
    public String getDescription() {
        return """
               Execute a SPARQL Update operation against the IQ knowledge graph.
               Supports INSERT DATA, DELETE DATA, DELETE/INSERT WHERE, and CLEAR.
               
               Requires authentication. Rate-limited to 10 calls/minute.
               
               Example — add a triple:
                 INSERT DATA { <https://example.org/s> <https://example.org/p> "value" . }
               
               Example — conditional update:
                 DELETE { ?s <p> ?old } INSERT { ?s <p> "new" } WHERE { ?s <p> ?old }
               
               DROP GRAPH operations require the additional param confirmDrop=true.
               """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "update",      Map.of("type", "string",  "description", "The SPARQL 1.1 Update string."),
                "realm",       Map.of("type", "string",  "description", "Target realm. Defaults to the default realm."),
                "confirmDrop", Map.of("type", "boolean", "description", "Set true to confirm a DROP GRAPH operation.")
            ),
            "required", List.of("update")
        );
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return Map.of(
            "type", "object",
            "description", "Result of SPARQL Update operation",
            "properties", Map.of(
                "status", Map.of("type", "string", "enum", List.of("ok", "error"), "description", "Operation status"),
                "trace", Map.of("type", "string", "description", "Trace ID for audit logging"),
                "message", Map.of("type", "string", "description", "Optional message or error details")
            )
        );
    }

    @Override
    public List<Map<String, Object>> getExamples() {
        return List.of(
            Map.of(
                "description", "Insert a single triple",
                "input", Map.of("update", "INSERT DATA { <https://example.org/subject> <https://example.org/predicate> \"value\" . }"),
                "output", Map.of("status", "ok")
            ),
            Map.of(
                "description", "Conditional delete and insert",
                "input", Map.of("update", "DELETE { ?s <http://example.org/status> ?old } INSERT { ?s <http://example.org/status> \"active\" } WHERE { ?s <http://example.org/type> \"user\" . OPTIONAL { ?s <http://example.org/status> ?old } }"),
                "output", Map.of("status", "ok")
            ),
            Map.of(
                "description", "Drop a named graph (requires confirmation)",
                "input", Map.of("update", "DROP GRAPH <http://example.org/temp>", "confirmDrop", true),
                "output", Map.of("status", "ok")
            )
        );
    }

    @Override
    public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
        String update = (String) input.get("update");
        if (update == null || update.isBlank()) throw MCPException.badRequest("'update' is required");

        if (!UPDATE_PATTERN.matcher(update).find()) {
            throw MCPException.badRequest("sparql.update requires a write operation (INSERT/DELETE/DROP/…)");
        }

        if (DROP_PATTERN.matcher(update).find()) {
            Object confirm = input.get("confirmDrop");
            if (!Boolean.TRUE.equals(confirm)) {
                throw MCPException.badRequest("DROP GRAPH requires confirmDrop=true to prevent accidental data loss");
            }
        }

        log.info("[sparql.update] principal={} [trace={}]", ctx.principal(), ctx.traceId());

        try (RepositoryConnection conn = repository.getConnection()) {
            conn.prepareUpdate(update).execute();
        } catch (Exception ex) {
            log.error("[sparql.update] failed [trace={}]", ctx.traceId(), ex);
            throw MCPException.internal("SPARQL update failed: " + ex.getMessage(), ex);
        }

        return MCPResult.okJson("{\"status\":\"ok\",\"trace\":\"" + ctx.traceId() + "\"}");
    }
}
