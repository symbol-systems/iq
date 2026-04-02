/**
 * JarScriptCatalog provides methods for managing SPARQL queries and templates.
 * It includes functionality to retrieve SPARQL queries from the JAR classpath,
 * apply Handlebars templates, and perform query lookups (scripts are scoped to MIME type and context).
 */
package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.COMMONS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.store.IQStore;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.render.HBSRenderer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class JarScriptCatalog implements I_Contents {
private static final Logger log = LoggerFactory.getLogger(JarScriptCatalog.class);
public static IRI HAS_CONTENT = RDF.VALUE;
public static IRI SPARQL_MIME = IQStore.vf.createIRI("urn:" + COMMONS.MIME_SPARQL);

/**
 * Constructs a ScriptCatalog instance.
 *
 * @param iq The IQ instance representing the JAR.
 */
public JarScriptCatalog() {
}

/**
 * Retrieves a SPARQL query based on the provided query path and MIME type.
 *
 * @param queryPath The path or IRI of the SPARQL query.
 * @return The SPARQL query as a string.
 */
public String getSPARQL(String queryPath) {
Literal content;
if (!queryPath.contains(":"))
content = getContent(IQStore.vf.createIRI(queryPath), SPARQL_MIME);
else
content = getContent(Values.iri(queryPath), SPARQL_MIME);
return content == null ? null : content.stringValue();
}

/**
 * Retrieves a SPARQL query (template) and injects the bindings.
 *
 * @param queryPath The path or IRI of the SPARQL query (template).
 * @param bindings  The template bindings.
 * @return The SPARQL query with bindings interpolated.
 * @throws IOException If an error occurs during template rendering.
 */
public String getSPARQL(String queryPath, Map<String, Object> bindings) throws IOException {
String query = getSPARQL(queryPath);
return query == null || bindings == null ? query : HBSRenderer.template(query, bindings);
}

/**
 * Retrieves a SPARQL query based on the provided IRI and MIME type.
 *
 * @param query The IRI representing the SPARQL query.
 * @return The SPARQL query as a string.
 */
public String getSPARQL(IRI query) {
Literal content = getContent(query, SPARQL_MIME);
return content == null ? null : content.stringValue();
}

/**
 * Retrieves a script based on the provided IRI, MIME type, and context.
 *
 * Supports loading from JAR resources using IRIs with the pattern:
 *   urn:iq:script:cli:builtin:boot-list-actors
 * maps to:
 *   /sparql/cli/builtin/boot-list-actors.sparql
 *
 * @param queryThe IRI representing the SPARQL query.
 * @param mimetype The MIME type associated with the query.
 * @return The SPARQL query as a Literal, or null if not found.
 */
@Override
public Literal getContent(Resource query, IRI mimetype) {
log.debug("sparql.getContent: {} -> {}", query, mimetype);

if (query == null) {
return null;
}

// Only load SPARQL MIME type
if (!mimetype.equals(SPARQL_MIME)) {
log.debug("getContent: skipping non-SPARQL MIME type: {}", mimetype);
return null;
}

String iriString = query.stringValue();
String resourcePath = iriToResourcePath(iriString, "sparql");

try {
String content = loadResource(resourcePath);
if (content != null) {
log.debug("Loaded SPARQL from JAR resource: {}", resourcePath);
ValueFactory vf = IQStore.vf;
return vf.createLiteral(content);
}
} catch (Exception e) {
log.debug("Failed to load resource {}: {}", resourcePath, e.getMessage());
}

return null;
}

/**
 * Converts an IRI to a JAR resource path.
 *
 * Example:
 *   urn:iq:script:cli:builtin:boot-list-actors → /sparql/cli/builtin/boot-list-actors.sparql
 *   urn:iq:script:agents:transitions → /sparql/agents/transitions.sparql
 *
 * @param iriThe IRI string
 * @param folder The resource folder (e.g., "sparql", "groovy")
 * @return The JAR resource path
 */
private String iriToResourcePath(String iri, String folder) {
// Parse URN: urn:iq:script:cli:builtin:boot-list-actors
if (iri.startsWith("urn:iq:script:")) {
String remainder = iri.substring("urn:iq:script:".length());
// Replace colons with slashes: cli:builtin:boot-list-actors → cli/builtin/boot-list-actors
String pathPart = remainder.replace(":", "/");
return "/" + folder + "/" + pathPart + ".sparql";
} else if (iri.startsWith("urn:")) {
// Handle other URNs
String remainder = iri.substring("urn:".length());
String pathPart = remainder.replace(":", "/");
return "/" + folder + "/" + pathPart + ".sparql";
} else {
// Assume it's a path-like reference
return "/" + folder + "/" + iri + ".sparql";
}
}

/**
 * Loads content from a JAR resource using the classloader.
 *
 * @param resourcePath The resource path (e.g., "/sparql/cli/builtin/boot-list-actors.sparql")
 * @return The resource content as a string, or null if not found
 * @throws IOException If an I/O error occurs
 */
private String loadResource(String resourcePath) throws IOException {
try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
if (is == null) {
log.trace("Resource not found: {}", resourcePath);
return null;
}
return new String(is.readAllBytes(), StandardCharsets.UTF_8);
}
}

}
