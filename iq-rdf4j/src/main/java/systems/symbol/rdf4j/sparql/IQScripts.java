package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import static systems.symbol.rdf4j.sparql.IQScriptCatalog.HAS_CONTENT;
import static systems.symbol.rdf4j.sparql.IQScriptCatalog.SPARQL_MIME;

public class IQScripts {
private static final Logger log = LoggerFactory.getLogger(IQScripts.class);

/**
 * Retrieves a SPARQL query based on the provided IRI, context, and MIME type.
 *
 * @param connection The RDF4J RepositoryConnection to perform the query.
 * @param query  The IRI representing the SPARQL query.
 * @param contextThe IRI representing the context in which the query should
 *   be executed.
 * @return The SPARQL query as a string.
 */
public static String getSPARQL(RepositoryConnection connection, IRI query, IRI context) {
Literal script = findScript(connection, query, SPARQL_MIME, context);
return script != null ? script.stringValue() : null;
}

/**
 * Retrieves a Script based on the provided IRI, MIME type, and context.
 *
 * @param connection The RDF4J RepositoryConnection to perform the script.
 * @param script The IRI representing the SPARQL script.
 * @param mimetype   The MIME type associated with the script.
 * @param contextThe IRI representing the context in which the script should
 *   be executed.
 * @return The SPARQL script as a string.
 */
public static Literal findScript(RepositoryConnection connection, Resource script, IRI mimetype, IRI context) {
log.debug("findScript.conn: {} -> {} -> {}", script, mimetype, context);
try (RepositoryResult<Statement> result = context == null ? connection.getStatements(script, HAS_CONTENT, null)
: connection.getStatements(script, HAS_CONTENT, null, context)) {
return findScript(result.iterator(), script, mimetype);
}
}

public static Literal findScript(Iterator<Statement> statements, Resource script, IRI mimetype) {
// log.info("findScript.seek: {} -> {} ==> {}", script, mimetype,
// statements.hasNext());

while (statements.hasNext()) {
Statement s = statements.next();
if (s.getObject() instanceof Literal) {
// log.debug("findScript.result: {}", s.getObject());
if (mimetype == null)
return (Literal) s.getObject();
if (mimetype.equals(((Literal) s.getObject()).getDatatype())) {
// log.debug("findScript.found: {} -> {}", s.getSubject(), null!=s.getObject());
return (Literal) s.getObject();
}
}
}
log.warn("findScript.missing: {}", script);
return null;
}

/**
 * Retrieves a Script based on the provided IRI, MIME type, and context.
 *
 * @param modelThe RDF4J Statement Model containing the script.
 * @param script   The IRI representing the script.
 * @param mimetype The MIME type associated with the script.
 * @param context  The IRI representing the context in which the script should
 * be executed. NUll matches any.
 * @return The script as a string.
 */
public static Literal findScript(@NotNull Model model, @NotNull Resource script, IRI mimetype, IRI context) {
if (script instanceof Literal) {
return (Literal) script;
}
Iterable<Statement> statements = context == null ? model.getStatements(script, HAS_CONTENT, null)
: model.getStatements(script, HAS_CONTENT, null, context);
Iterator<Statement> statementIterator = statements.iterator();
log.info("findScript.model: {} = {} @ {} -> {}", script, statementIterator.hasNext(), mimetype, context);
return findScript(statementIterator, script, mimetype);
}

public static GraphQueryResult describe(RepositoryConnection connection, String self) {
GraphQuery graphQuery = connection.prepareGraphQuery("DESCRIBE <" + self + ">");
return graphQuery.evaluate();
}
}
