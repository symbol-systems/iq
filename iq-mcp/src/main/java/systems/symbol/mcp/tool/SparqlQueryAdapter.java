package systems.symbol.mcp.tool;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SparqlQueryAdapter — Pillar A: the universal read tool.
 *
 * <p>Accepts a SPARQL string and routes it to either a {@code TupleQuery}
 * (SELECT) or a {@code GraphQuery} (CONSTRUCT / DESCRIBE).  Results are
 * serialised as JSON (SELECT) or Turtle (CONSTRUCT/DESCRIBE).
 *
 * <p>SPARQL write keywords are blocked upstream by
 * {@link systems.symbol.mcp.connect.impl.SparqlSafetyMiddleware}.
 *
 * <p>Tool name: {@value systems.symbol.mcp.MCP_NS#TOOL_SPARQL_QUERY}
 */
public class SparqlQueryAdapter implements I_MCPTool {

    private static final Logger log = LoggerFactory.getLogger(SparqlQueryAdapter.class);

    private final Repository repository;

    public SparqlQueryAdapter(Repository repository) {
        this.repository = repository;
    }

    @Override public String getName()        { return "sparql.query"; }
    @Override public boolean isReadOnly()    { return true; }
    @Override public int defaultRateLimit()  { return 100; }
    @Override public int order()             { return 10; }

    @Override
    public String getDescription() {
        return """
               Execute a SPARQL SELECT, CONSTRUCT, or DESCRIBE query against the IQ knowledge graph.
               
               Use SELECT for tabular data retrieval.
               Use CONSTRUCT or DESCRIBE to retrieve subgraphs (returns Turtle).
               
               For schema exploration: SELECT ?class WHERE { ?class a owl:Class } LIMIT 50
               For entity lookup:      DESCRIBE <https://example.org/entity/123>
               For counts:             SELECT (COUNT(?s) AS ?count) WHERE { ?s a :MyType }
               
               Results are paginated to 1000 rows maximum.
               """;
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "query",  Map.of("type", "string",  "description", "The SPARQL 1.1 query string (SELECT, CONSTRUCT, DESCRIBE, or ASK)"),
                "realm",  Map.of("type", "string",  "description", "Optional realm/repository to query. Defaults to the default realm."),
                "format", Map.of("type", "string",  "description", "Output format: 'json' (default for SELECT), 'turtle' (for CONSTRUCT/DESCRIBE).")
            ),
            "required", List.of("query")
        );
    }

    @Override
    public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
        // Prefer the safety-rewritten query if available
        String query = ctx.has("mcp.sparql.query")
                ? ctx.get("mcp.sparql.query")
                : (String) input.get("query");

        if (query == null || query.isBlank()) throw MCPException.badRequest("'query' is required");

        log.debug("[sparql.query] executing [trace={}]", ctx.traceId());

        try (RepositoryConnection conn = repository.getConnection()) {
            String upper = query.trim().toUpperCase();
            
            // Determine query type by finding first occurrence of major keyword
            // (rather than just checking start, to handle PREFIX declarations)
            boolean isSelectOrAsk = upper.contains("SELECT") || upper.contains("ASK");
            
            // Find if the main keyword is SELECT/ASK vs CONSTRUCT/DESCRIBE
            int selectIdx = upper.indexOf("SELECT");
            int askIdx = upper.indexOf("ASK");
            int constructIdx = upper.indexOf("CONSTRUCT");
            int describeIdx = upper.indexOf("DESCRIBE");
            
            // Find the earliest occurring main keyword (that's not in a comment)
            int mainKeywordIdx = Integer.MAX_VALUE;
            boolean isRowBased = false;
            
            if (selectIdx >= 0 && selectIdx < mainKeywordIdx) {
                mainKeywordIdx = selectIdx;
                isRowBased = true;
            }
            if (askIdx >= 0 && askIdx < mainKeywordIdx) {
                mainKeywordIdx = askIdx;
                isRowBased = true;
            }
            if (constructIdx >= 0 && constructIdx < mainKeywordIdx) {
                mainKeywordIdx = constructIdx;
                isRowBased = false;
            }
            if (describeIdx >= 0 && describeIdx < mainKeywordIdx) {
                mainKeywordIdx = describeIdx;
                isRowBased = false;
            }

            if (isRowBased) {
                return executeSelect(conn, query);
            } else {
                return executeGraph(conn, query);
            }
        } catch (MCPException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[sparql.query] execution failed [trace={}]", ctx.traceId(), ex);
            throw MCPException.internal("SPARQL execution failed: " + ex.getMessage(), ex);
        }
    }

    private I_MCPResult executeSelect(RepositoryConnection conn, String query) throws Exception {
        try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
            List<String> vars    = result.getBindingNames();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (result.hasNext()) {
                BindingSet bs = result.next();
                Map<String, Object> row = new LinkedHashMap<>();
                for (String v : vars) {
                    var val = bs.getValue(v);
                    row.put(v, val != null ? val.stringValue() : null);
                }
                rows.add(row);
            }
            StringBuilder json = new StringBuilder();
            json.append("{\"vars\":").append(toJsonArray(vars))
                .append(",\"results\":").append(toJsonArray(rows)).append("}");
            return MCPResult.okJson(json.toString());
        }
    }

    private I_MCPResult executeGraph(RepositoryConnection conn, String query) throws Exception {
        try (GraphQueryResult result = conn.prepareGraphQuery(query).evaluate()) {
            var model = QueryResults.asModel(result);
            StringWriter sw = new StringWriter();
            Rio.write(model, sw, RDFFormat.TURTLE);
            return MCPResult.ok(sw.toString(), "text/turtle");
        }
    }

    /* ── trivial JSON helpers — replace with Jackson if available ─────────── */

    private static String toJsonArray(List<?> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(',');
            Object item = items.get(i);
            if (item instanceof String s) sb.append('"').append(escape(s)).append('"');
            else if (item instanceof Map<?,?> m) sb.append(mapToJson(m));
            else sb.append('"').append(item).append('"');
        }
        return sb.append(']').toString();
    }

    @SuppressWarnings("unchecked")
    private static String mapToJson(Map<?, ?> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey().toString())).append("\":");
            if (e.getValue() == null) sb.append("null");
            else sb.append('"').append(escape(e.getValue().toString())).append('"');
        }
        return sb.append('}').toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
