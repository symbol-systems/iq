/**
 * ScriptCatalog provides methods for managing SPARQL queries and templates.
 * It includes functionality to retrieve SPARQL queries from the knowledge base,
 * apply Handlebars templates, and perform query lookups (scripts are scoped to MIME type and context).
 */
package systems.symbol.rdf4j.sparql;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.iq.I_Contents;
import systems.symbol.render.HBSRenderer;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


public class ScriptCatalog implements I_Contents {
private static final Logger log = LoggerFactory.getLogger(ScriptCatalog.class);
public static IRI HAS_CONTENT = RDF.VALUE;
public static IRI SPARQL_MIME = IQ.vf.createIRI(COMMONS.MIME_SPARQL);
IQ iq;

/**
 * Constructs a ScriptCatalog instance.
 *
 * @param iq The IQ instance representing the knowledge base connection.
 */
public ScriptCatalog(IQ iq) {
this.iq = iq;
}

/**
 * Retrieves a SPARQL query based on the provided query path and MIME type.
 *
 * @param queryPath The path or IRI of the SPARQL query.
 * @return The SPARQL query as a string.
 */
public String getSPARQL(String queryPath) {
if (!queryPath.contains(":")) return getContent(iq.toIRI(queryPath), SPARQL_MIME);
return getContent(IQ.vf.createIRI(queryPath), SPARQL_MIME);
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
return query==null||bindings==null?query:HBSRenderer.template(query, bindings);
}

/**
 * Retrieves a SPARQL query based on the provided IRI and MIME type.
 *
 * @param query The IRI representing the SPARQL query.
 * @return The SPARQL query as a string.
 */
public String getSPARQL(IRI query) {
return getContent(query, SPARQL_MIME);
}

/**
 * Retrieves a script based on the provided IRI, MIME type, and context.
 *
 * @param queryThe IRI representing the SPARQL query.
 * @param mimetype The MIME type associated with the query.
 * @return The SPARQL query as a string.
 */
@Override
public String getContent(Resource query, IRI mimetype) {
Literal script = findScript(iq.getConnection(), query, mimetype, iq.getIdentity());
return script!=null?script.stringValue():null;
}

/**
 * Retrieves a SPARQL query based on the provided IRI, context, and MIME type.
 *
 * @param connection The RDF4J RepositoryConnection to perform the query.
 * @param query  The IRI representing the SPARQL query.
 * @param contextThe IRI representing the context in which the query should be executed.
 * @return The SPARQL query as a string.
 */
public static String getSPARQL(RepositoryConnection connection, IRI query, IRI context) {
Literal script = findScript(connection, query, SPARQL_MIME, context);
return script!=null?script.stringValue():null;
}

/**
 * Retrieves a Script based on the provided IRI, MIME type, and context.
 *
 * @param connection The RDF4J RepositoryConnection to perform the script.
 * @param script  The IRI representing the SPARQL script.
 * @param mimetype   The MIME type associated with the script.
 * @param contextThe IRI representing the context in which the script should be executed.
 * @return The SPARQL script as a string.
 */
public static Literal findScript(RepositoryConnection connection, Resource script, IRI mimetype, IRI context) {
log.debug("library.script: {} -> {} -> {}", script, mimetype, context);

try (RepositoryResult<Statement> result = connection.getStatements(script, HAS_CONTENT, null, context)) {
return findScript(result.iterator(), script, mimetype);
//while (result.hasNext()) {
//Statement s = result.next();
////log.debug("library.script.result: {}", s.getObject());
//if (s.getObject() instanceof Literal) {
//if (mimetype==null) return s.getObject().stringValue();
//else if (((Literal) s.getObject()).getDatatype().equals(mimetype)) {
////log.info("library.script.found: {} -> {}", s.getSubject(), s.getObject());
//return s.getObject().stringValue();
//}
//}
//}
}
//return null;
}

public static Literal findScript(Iterator<Statement> statements, Resource script, IRI mimetype) {
log.debug("library.script: {} -> {}", script, mimetype);

while (statements.hasNext()) {
Statement s = statements.next();
log.debug("library.script.result: {}", s.getObject());
if (s.getObject() instanceof Literal) {
if (mimetype==null) return (Literal)s.getObject();
else if (mimetype.equals( ((Literal) s.getObject()).getDatatype())) {
log.info("library.script.found: {} -> {}", s.getSubject(), s.getObject());
return (Literal)s.getObject();
}
}
}
return null;
}

/**
 * Retrieves a Script based on the provided IRI, MIME type, and context.
 *
 * @param model The RDF4J Statement Model containing the script.
 * @param script  The IRI representing the script.
 * @param mimetype   The MIME type associated with the script.
 * @param contextThe IRI representing the context in which the script should be executed. NUll matches any.
 * @return The script as a string.
 */
public static Literal findScript(@NotNull  Model model, @NotNull Resource script, IRI mimetype, IRI context) {
Iterable<Statement> statements = context==null?model.getStatements(script, HAS_CONTENT, null):model.getStatements(script, HAS_CONTENT, null, context);
Iterator<Statement> statementIterator = statements.iterator();
log.info("library.script: {} -> {} -> {}", script, mimetype, statementIterator.hasNext());
return findScript(statementIterator, script, mimetype);
}
}
