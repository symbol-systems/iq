package systems.symbol.mcp.resource;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResource;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

/**
 * VoidResourceProvider — serves the VoID (Vocabulary of Interlinked Datasets) description.
 *
 * <p>URI: {@code iq://self/void}
 *
 * <p>VoID describes what datasets/graphs exist, their sizes, and links.
 * This gives the LLM a spatial map of the graph before it writes SPARQL.
 * Content is generated from the RDF4J repository introspection.
 */
public class VoidResourceProvider implements I_MCPResource {

private static final Logger log = LoggerFactory.getLogger(VoidResourceProvider.class);
private static final String URI = "iq://self/void";

private final Repository repository;

public VoidResourceProvider(Repository repository) { this.repository = repository; }

@Override public String getUri() { return URI; }
@Override public String getLabel()   { return "VoID Dataset Description"; }
@Override public String getMimeType(){ return "text/turtle"; }
@Override
public String getDescription() {
return "VoID dataset description listing all named graphs with triple counts. " +
   "Use this to understand which datasets are available and how large they are.";
}

@Override
public I_MCPResult read(MCPCallContext ctx, String uri) throws MCPException {
log.debug("[VoidResource] building VoID [trace={}]", ctx.traceId());
try (RepositoryConnection conn = repository.getConnection()) {
StringBuilder ttl = new StringBuilder();
ttl.append("@prefix void: <http://rdfs.org/ns/void#> .\n");
ttl.append("@prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .\n\n");
ttl.append("<iq://self> a void:DatasetDescription ;\n");
ttl.append("  void:subset ");

var result = conn.prepareTupleQuery("""
SELECT ?g (COUNT(*) AS ?count) WHERE {
GRAPH ?g { ?s ?p ?o }
}
GROUP BY ?g
ORDER BY DESC(?count)
""").evaluate();

boolean firstGraph = true;
while (result.hasNext()) {
var bs= result.next();
String g  = bs.getValue("g").stringValue();
String cnt= bs.getValue("count").stringValue();
if (!firstGraph) ttl.append(",\n ");
firstGraph = false;
ttl.append("[\na void:Dataset ;\n");
ttl.append("void:sparqlEndpoint <iq://sparql> ;\n");
ttl.append("void:triples ").append(cnt).append(" ;\n");
ttl.append("void:rootResource <").append(g).append("> \n  ]");
}
result.close();
ttl.append(" .\n");
return MCPResult.ok(ttl.toString(), "text/turtle");
} catch (Exception ex) {
throw MCPException.internal("Failed to build VoID: " + ex.getMessage(), ex);
}
}
}
