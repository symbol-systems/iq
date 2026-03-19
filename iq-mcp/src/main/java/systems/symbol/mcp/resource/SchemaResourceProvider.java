package systems.symbol.mcp.resource;

import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResource;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.io.StringWriter;

/**
 * SchemaResourceProvider — serves the OWL/SHACL class model for a realm.
 *
 * <p>URI template: {@code iq://realm/{realm}/schema}
 *
 * <p>Why this is a Resource and not a Tool: schema content is purely
 * declarative, read-only, and should be loaded into the LLM's context
 * window before any query or write.
 *
 * <p>Content: Turtle serialisation of everything in the named graph
 * {@code <realm-id>/schema} (OWL class hierarchy, property declarations).
 */
public class SchemaResourceProvider implements I_MCPResource {

private static final Logger log = LoggerFactory.getLogger(SchemaResourceProvider.class);
private static final String URI_TEMPLATE = "iq://realm/{realm}/schema";

private final Repository repository;

public SchemaResourceProvider(Repository repository) { this.repository = repository; }

@Override public String getUri() { return URI_TEMPLATE; }
@Override public String getLabel()   { return "Realm Schema"; }
@Override public String getMimeType(){ return "text/turtle"; }
@Override
public String getDescription() {
return "OWL class hierarchy and property declarations for the specified realm. " +
   "Read this before writing SPARQL queries to understand the data model.";
}

@Override
public boolean matchesUri(String uri) {
return uri != null && uri.startsWith("iq://realm/") && uri.endsWith("/schema");
}

@Override
public I_MCPResult read(MCPCallContext ctx, String uri) throws MCPException {
String realm = extractRealm(uri);
if (realm == null) throw MCPException.badRequest("Could not extract realm from URI: " + uri);

log.debug("[SchemaResource] loading schema for realm={} [trace={}]", realm, ctx.traceId());

// CONSTRUCT all triples from the realm's schema named graph
String sparql = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <" + realm + "/schema> { ?s ?p ?o } }";

try (RepositoryConnection conn = repository.getConnection();
 GraphQueryResult result   = conn.prepareGraphQuery(sparql).evaluate()) {
var model = QueryResults.asModel(result);
if (model.isEmpty()) return MCPResult.notFound(uri);
StringWriter sw = new StringWriter();
Rio.write(model, sw, RDFFormat.TURTLE);
return MCPResult.ok(sw.toString(), "text/turtle");
} catch (Exception ex) {
throw MCPException.internal("Failed to load schema for realm '" + realm + "': " + ex.getMessage(), ex);
}
}

private static String extractRealm(String uri) {
// iq://realm/{realm}/schema → realm
try {
String path = uri.substring("iq://realm/".length());
return path.substring(0, path.lastIndexOf("/schema"));
} catch (Exception ex) { return null; }
}
}
