package systems.symbol.mcp.connect.impl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.mcp.MCP_NS;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.connect.I_MCPPipeline;
import systems.symbol.mcp.connect.MCPChain;

import java.time.Instant;

/**
 * AuditWriterMiddleware — writes structured audit events for tool calls (order: 90).
 *
 * <p>Runs after the adapter by wrapping the chain: any result (success or error)
 * is recorded as RDF triples in the {@code mcp:audit} named graph.
 *
 * <p>Audit triple shape:
 * <pre>
 * {@code <urn:mcp:audit/{traceId}> a mcp:AuditEvent ;
 *   mcp:principal "user@example.com" ;
 *   mcp:tool "sparql.query" ;
 *   mcp:timestamp "2026-03-19T12:00:00Z"^^xsd:dateTime ;
 *   mcp:durationMs 42 ;
 *   mcp:outcome "success" .}
 * </pre>
 *
 * <p>No-arg constructor skips writes (testing / no-repo mode).
 */
public class AuditWriterMiddleware implements I_MCPPipeline {

private static final IRI SELF = Values.iri("urn:mcp:pipeline/AuditWriter");

private final Repository repository;

public AuditWriterMiddleware() { this.repository = null; }
public AuditWriterMiddleware(Repository repository) { this.repository = repository; }

@Override public IRI getSelf()  { return SELF; }
@Override public int getOrder() { return 90; }

@Override
public I_MCPResult process(MCPCallContext ctx, MCPChain chain) throws MCPException {
I_MCPResult result;
String outcome = "success";
try {
result = ctx.get("mcp.result");
if (result == null) result = chain.proceed(ctx);
if (result.isError()) outcome = "error";
} catch (MCPException ex) {
outcome = "error";
writeAudit(ctx, outcome, ex.getMessage());
throw ex;
}
writeAudit(ctx, outcome, null);
return result;
}

private void writeAudit(MCPCallContext ctx, String outcome, String errorMsg) {
if (repository == null) {
return; // no-repo mode
}
try (RepositoryConnection conn = repository.getConnection()) {
var vf= SimpleValueFactory.getInstance();
IRI eventIRI  = vf.createIRI("urn:mcp:audit/" + ctx.traceId());
long durationMs = java.time.Duration.between(ctx.startTime(), Instant.now()).toMillis();
Model model   = new LinkedHashModel();
model.add(eventIRI, vf.createIRI(MCP_NS.MCP, "principal"),  vf.createLiteral(ctx.principal() != null ? ctx.principal() : "anonymous"), MCP_NS.GRAPH_AUDIT);
model.add(eventIRI, vf.createIRI(MCP_NS.MCP, "tool"),vf.createLiteral(ctx.toolName()), MCP_NS.GRAPH_AUDIT);
model.add(eventIRI, vf.createIRI(MCP_NS.MCP, "timestamp"),   vf.createLiteral(Instant.now().toString()), MCP_NS.GRAPH_AUDIT);
model.add(eventIRI, vf.createIRI(MCP_NS.MCP, "durationMs"),  vf.createLiteral(durationMs), MCP_NS.GRAPH_AUDIT);
model.add(eventIRI, vf.createIRI(MCP_NS.MCP, "outcome"), vf.createLiteral(outcome), MCP_NS.GRAPH_AUDIT);
if (errorMsg != null)
model.add(eventIRI, vf.createIRI(MCP_NS.MCP, "error"), vf.createLiteral(errorMsg), MCP_NS.GRAPH_AUDIT);
conn.add(model);
} catch (Exception ex) {
// silently fail on audit persistence errors; don't interrupt request
}
}
}
