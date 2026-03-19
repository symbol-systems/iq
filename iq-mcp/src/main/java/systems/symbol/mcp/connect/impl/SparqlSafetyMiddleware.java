package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.mcp.MCP_NS;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * SparqlSafetyMiddleware — prevents write abuse in read-only tools (order: 45).
 *
 * <p>For the {@code sparql.query} tool, this middleware rejects SPARQL strings
 * containing write keywords (INSERT, DELETE, DROP, CREATE, LOAD, etc.)
 * to prevent misuse as a write path.
 *
 * <p>Additionally, it enforces a hard LIMIT 1000 on SELECT queries
 * without a declared limit to prevent LLM context window overflow.
 */
public class SparqlSafetyMiddleware implements I_MCPPipeline {

    private static final IRI SELF = Values.iri("urn:mcp:pipeline/SparqlSafety");
    private static final Pattern WRITE_KEYWORDS = Pattern.compile(
            "(?i)\\b(INSERT|DELETE|DROP|CREATE|LOAD|CLEAR|COPY|MOVE|ADD)\\b");
    private static final int MAX_LIMIT = 1000;
    private static final Pattern LIMIT_CLAUSE = Pattern.compile("(?i)\\bLIMIT\\b");

    @Override public IRI getSelf()  { return SELF; }
    @Override public int getOrder() { return 45; }

    @Override
    public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
        if (!MCP_NS.TOOL_SPARQL_QUERY.equals(ctx.toolName())) {
            return chain.proceed(ctx);
        }

        Map<String, Object> input = ctx.rawInput();
        String query = input.containsKey("query") ? input.get("query").toString() : null;
        if (query == null || query.isBlank()) {
            throw MCPException.badRequest("missing query parameter");
        }

        if (WRITE_KEYWORDS.matcher(query).find()) {
            throw MCPException.forbidden("write operations not permitted");
        }

        if (query.trim().toUpperCase().startsWith("SELECT") && !LIMIT_CLAUSE.matcher(query).find()) {
            query = query + "\nLIMIT " + MAX_LIMIT;
            ctx.set("mcp.sparql.query", query);
        }

        return chain.proceed(ctx);
    }
}
