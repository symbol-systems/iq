package systems.symbol.rdf4j.fedx;

import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * REST API for federated SPARQL queries.
 * Exposes FedXRepository via HTTP endpoints following SPARQL Protocol.
 *
 * Query Parameters:
 *   - query: SPARQL query string (required)
 *   - format: Result format (json, xml, csv) - default: json
 *   - timeout: Query timeout in seconds (optional)
 */
public class FedXAPI {

private static final Logger log = LoggerFactory.getLogger(FedXAPI.class);

private final I_FedXRepository repository;

public FedXAPI(I_FedXRepository repository) {
this.repository = Objects.requireNonNull(repository, "repository");
}

/**
 * Execute a SELECT query and return results in JSON format.
 * Implements SPARQL Protocol for query operations.
 *
 * Endpoint: GET/POST /sparql/federated?query=...
 *
 * @param query SPARQL SELECT query (URL-encoded)
 * @param timeout Query timeout in seconds (optional)
 * @return SPARQL JSON results
 */
public String selectQuery(String query, Integer timeout) throws RepositoryException {
Objects.requireNonNull(query, "query parameter required");

log.debug("Executing federated SELECT query, timeout={}", timeout);

try {
TupleQuery tupleQuery = repository.prepareTupleQuery(query);

if (timeout != null && timeout > 0) {
tupleQuery.setMaxExecutionTime(timeout);
}

TupleQueryResult result = tupleQuery.evaluate();
String jsonResults = formatTupleResultsAsJSON(result);
result.close();

return jsonResults;
} catch (QueryEvaluationException e) {
throw new RepositoryException("SELECT query evaluation failed", e);
}
}

/**
 * Execute an ASK query and return boolean result.
 *
 * Endpoint: GET/POST /sparql/federated?query=...
 *
 * @param query SPARQL ASK query (URL-encoded)
 * @param timeout Query timeout in seconds (optional)
 * @return JSON with boolean result: { "boolean": true/false }
 */
public String askQuery(String query, Integer timeout) throws RepositoryException {
Objects.requireNonNull(query, "query parameter required");

log.debug("Executing federated ASK query");

try {
BooleanQuery booleanQuery = repository.prepareBooleanQuery(query);

if (timeout != null && timeout > 0) {
booleanQuery.setMaxExecutionTime(timeout);
}

boolean result = booleanQuery.evaluate();
return formatBooleanAsJSON(result);
} catch (QueryEvaluationException e) {
throw new RepositoryException("ASK query evaluation failed", e);
}
}

/**
 * Execute a CONSTRUCT query and return RDF results.
 * Merges results from all federated endpoints into a single model.
 *
 * @param query SPARQL CONSTRUCT query
 * @param timeout Query timeout in seconds (optional)
 * @return RDF triples in Turtle format
 */
public String constructQuery(String query, Integer timeout) throws RepositoryException {
Objects.requireNonNull(query, "query parameter required");

log.debug("Executing federated CONSTRUCT query");

try {
GraphQuery graphQuery = repository.prepareGraphQuery(query);

if (timeout != null && timeout > 0) {
graphQuery.setMaxExecutionTime(timeout);
}

GraphQueryResult result = graphQuery.evaluate();
org.eclipse.rdf4j.model.Model constructedModel = new org.eclipse.rdf4j.model.impl.LinkedHashModel();

// Collect all statements from the federated result
while (result.hasNext()) {
constructedModel.add(result.next());
}
result.close();

// Serialize to Turtle format
ByteArrayOutputStream out = new ByteArrayOutputStream();
org.eclipse.rdf4j.rio.RDFWriter writer = org.eclipse.rdf4j.rio.Rio.createWriter(
org.eclipse.rdf4j.rio.RDFFormat.TURTLE, out);
writer.startRDF();
for (org.eclipse.rdf4j.model.Statement stmt : constructedModel) {
writer.handleStatement(stmt);
}
writer.endRDF();

return out.toString("UTF-8");
} catch (QueryEvaluationException e) {
throw new RepositoryException("CONSTRUCT query evaluation failed", e);
} catch (IOException e) {
throw new RepositoryException("CONSTRUCT result serialization failed", e);
}
}

/**
 * Format TupleQueryResult as SPARQL JSON.
 * Output: { "head": { "vars": [...] }, "results": { "bindings": [...] } }
 */
private String formatTupleResultsAsJSON(TupleQueryResult result) throws QueryEvaluationException {
StringBuilder json = new StringBuilder();
json.append("{");
json.append("\"head\":{\"vars\":[");

// Write variable names
List<String> varNames = result.getBindingNames();
for (int i = 0; i < varNames.size(); i++) {
if (i > 0) json.append(",");
json.append("\"").append(escapeJSON(varNames.get(i))).append("\"");
}

json.append("]},");
json.append("\"results\":{\"bindings\":[");

// Write binding sets
boolean firstBinding = true;
while (result.hasNext()) {
if (!firstBinding) json.append(",");
firstBinding = false;

BindingSet binding = result.next();
json.append("{");

boolean firstVar = true;
for (String varName : varNames) {
if (binding.hasBinding(varName)) {
if (!firstVar) json.append(",");
firstVar = false;

org.eclipse.rdf4j.model.Value value = binding.getValue(varName);
json.append("\"").append(escapeJSON(varName)).append("\":");
json.append(formatRDFValue(value));
}
}

json.append("}");
}

json.append("]}}");
return json.toString();
}

/**
 * Format boolean result as SPARQL JSON.
 * Output: { "head": {}, "boolean": true/false }
 */
private String formatBooleanAsJSON(boolean result) {
return "{\"head\":{},\"boolean\":" + result + "}";
}

/**
 * Format an RDF Value in SPARQL JSON format.
 * For URIs: { "type": "uri", "value": "http://..." }
 * For Literals: { "type": "***REMOVED***", "value": "..." }
 */
private String formatRDFValue(org.eclipse.rdf4j.model.Value value) {
if (value instanceof org.eclipse.rdf4j.model.IRI) {
org.eclipse.rdf4j.model.IRI iri = (org.eclipse.rdf4j.model.IRI) value;
return "{\"type\":\"uri\",\"value\":\"" + escapeJSON(iri.stringValue()) + "\"}";
} else if (value instanceof org.eclipse.rdf4j.model.Literal) {
org.eclipse.rdf4j.model.Literal lit = (org.eclipse.rdf4j.model.Literal) value;
return "{\"type\":\"***REMOVED***\",\"value\":\"" + escapeJSON(lit.stringValue()) + "\"}";
} else if (value instanceof org.eclipse.rdf4j.model.BNode) {
org.eclipse.rdf4j.model.BNode bnode = (org.eclipse.rdf4j.model.BNode) value;
return "{\"type\":\"bnode\",\"value\":\"" + escapeJSON(bnode.getID()) + "\"}";
} else {
return "{\"type\":\"unknown\",\"value\":\"" + escapeJSON(value.stringValue()) + "\"}";
}
}

/**
 * Escape special characters for JSON string output.
 */
private String escapeJSON(String value) {
return value
.replace("\\", "\\\\")
.replace("\"", "\\\"")
.replace("\n", "\\n")
.replace("\r", "\\r")
.replace("\t", "\\t");
}
}
