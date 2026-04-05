package systems.symbol.rdf4j.fedx;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP-based remote SPARQL client.
 * Executes queries against remote SPARQL endpoints via HTTP.
 *
 * Uses application/x-www-form-urlencoded encoding for queries.
 * Results are retrieved as JSON (SPARQL JSON format).
 */
public class HTTPRemoteSPARQLClient implements I_RemoteSPARQLClient {

private static final Logger log = LoggerFactory.getLogger(HTTPRemoteSPARQLClient.class);

private final HttpClient httpClient;
private final Map<String, Long> endpointLatencies = new ConcurrentHashMap<>();
private final long defaultTimeoutMs;

public HTTPRemoteSPARQLClient() {
this(5000);  // 5-second default timeout
}

public HTTPRemoteSPARQLClient(long timeoutMs) {
this.httpClient = HttpClients.createDefault();
this.defaultTimeoutMs = timeoutMs;
}

@Override
public List<BindingSet> selectQuery(FedXEndpoint endpoint, String query, QueryLanguage language)
throws RepositoryException {
try {
long startTime = System.currentTimeMillis();

String url = endpoint.sparqlEndpoint() + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
ClassicHttpRequest request = ClassicRequestBuilder.get(url)
.addHeader("Accept", "application/sparql-results+json")
.build();

List<BindingSet> results = httpClient.execute(request, new HttpClientResponseHandler<List<BindingSet>>() {
@Override
public List<BindingSet> handleResponse(ClassicHttpResponse response) throws IOException {
int status = response.getCode();
if (status < 200 || status >= 300) {
throw new IOException("HTTP " + status + " from " + endpoint.nodeId());
}

HttpEntity entity = response.getEntity();
String jsonResponse = "{}";
if (entity != null) {
try {
jsonResponse = EntityUtils.toString(entity);
} catch (org.apache.hc.core5.http.ParseException e) {
throw new IOException("Failed to parse response", e);
}
}
return parseSPARQLJsonResults(jsonResponse);
}
});

long latency = System.currentTimeMillis() - startTime;
endpointLatencies.put(endpoint.nodeId(), latency);

log.debug("SELECT query executed on {}: {} ms, {} results", endpoint.nodeId(), latency, results.size());
return results;
} catch (Exception e) {
throw new RepositoryException("SELECT query failed on " + endpoint.nodeId(), e);
}
}

@Override
public boolean askQuery(FedXEndpoint endpoint, String query, QueryLanguage language)
throws RepositoryException {
try {
long startTime = System.currentTimeMillis();

String url = endpoint.sparqlEndpoint() + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
ClassicHttpRequest request = ClassicRequestBuilder.get(url)
.addHeader("Accept", "application/sparql-results+json")
.build();

Boolean result = httpClient.execute(request, new HttpClientResponseHandler<Boolean>() {
@Override
public Boolean handleResponse(ClassicHttpResponse response) throws IOException {
int status = response.getCode();
if (status < 200 || status >= 300) {
throw new IOException("HTTP " + status + " from " + endpoint.nodeId());
}

HttpEntity entity = response.getEntity();
String jsonResponse = "{}";
if (entity != null) {
try {
jsonResponse = EntityUtils.toString(entity);
} catch (org.apache.hc.core5.http.ParseException e) {
throw new IOException("Failed to parse response", e);
}
}
return parseSPARQLJsonBoolean(jsonResponse);
}
});

long latency = System.currentTimeMillis() - startTime;
endpointLatencies.put(endpoint.nodeId(), latency);

log.debug("ASK query executed on {}: {} ms, result={}", endpoint.nodeId(), latency, result);
return result;
} catch (Exception e) {
throw new RepositoryException("ASK query failed on " + endpoint.nodeId(), e);
}
}

@Override
public boolean isEndpointReachable(FedXEndpoint endpoint) {
try {
ClassicHttpRequest request = ClassicRequestBuilder.head(endpoint.sparqlEndpoint())
.build();

var response = httpClient.executeOpen(null, request, null);
int status = response.getCode();
response.close();

return status >= 200 && status < 300;
} catch (Exception e) {
log.warn("Endpoint {} unreachable: {}", endpoint.nodeId(), e.getMessage());
return false;
}
}

@Override
public long getEndpointLatency(FedXEndpoint endpoint) {
// Try a test query (basic pattern)
try {
long startTime = System.currentTimeMillis();

String testQuery = "ASK WHERE { ?s ?p ?o } LIMIT 1";
String url = endpoint.sparqlEndpoint() + "?query=" + URLEncoder.encode(testQuery, StandardCharsets.UTF_8);
ClassicHttpRequest request = ClassicRequestBuilder.get(url)
.addHeader("Accept", "application/sparql-results+json")
.build();

var response = httpClient.executeOpen(null, request, null);
long latency = System.currentTimeMillis() - startTime;
response.close();

endpointLatencies.put(endpoint.nodeId(), latency);
return latency;
} catch (Exception e) {
log.debug("Failed to measure latency for {}: {}", endpoint.nodeId(), e.getMessage());
return -1;
}
}

/**
 * Parse SPARQL JSON results format into List<BindingSet>.
 * Format: { "head": { "vars": [...] }, "results": { "bindings": [...] } }
 */
private List<BindingSet> parseSPARQLJsonResults(String jsonResponse) {
List<BindingSet> bindings = new ArrayList<>();
try {
// Simple JSON parsing without external dependency
// Extract variables from "head"
Set<String> variables = extractVariables(jsonResponse);

// Extract bindings from "results.bindings"
int bindingsStart = jsonResponse.indexOf("\"bindings\"");
if (bindingsStart < 0) return bindings;

bindingsStart = jsonResponse.indexOf("[", bindingsStart);
int bindingsEnd = jsonResponse.lastIndexOf("]");
if (bindingsStart < 0 || bindingsEnd <= bindingsStart) return bindings;

String bindingsArray = jsonResponse.substring(bindingsStart + 1, bindingsEnd);

// Parse each binding object
int depth = 0;
int objectStart = 0;
for (int i = 0; i < bindingsArray.length(); i++) {
char c = bindingsArray.charAt(i);
if (c == '{') {
if (depth == 0) objectStart = i;
depth++;
} else if (c == '}') {
depth--;
if (depth == 0) {
String boundObject = bindingsArray.substring(objectStart, i + 1);
BindingSet binding = parseSingleBinding(boundObject, variables);
if (binding != null) {
bindings.add(binding);
}
}
}
}
} catch (Exception e) {
log.warn("Failed to parse SPARQL JSON results: {}", e.getMessage());
}
return bindings;
}

/**
 * Parse a single binding object: { "varName": { "type": "uri", "value": "..." }, ... }
 */
private BindingSet parseSingleBinding(String bindingJson, Set<String> variables) {
try {
MapBindingSet binding = new MapBindingSet(variables.size());

for (String var : variables) {
String varPattern = "\"" + var + "\"\\s*:\\s*\\{";
int varIdx = bindingJson.indexOf("\"" + var + "\"");
if (varIdx < 0) continue;

// Find the value for this variable
int valueStart = bindingJson.indexOf("\"value\"", varIdx);
if (valueStart < 0) continue;

int colonIdx = bindingJson.indexOf(":", valueStart);
int quoteStart = bindingJson.indexOf("\"", colonIdx);
int quoteEnd = bindingJson.indexOf("\"", quoteStart + 1);

if (quoteEnd <= quoteStart) continue;

String value = bindingJson.substring(quoteStart + 1, quoteEnd);

// For now, treat all values as simple ***REMOVED***s
// In production, inspect "type" field (uri, ***REMOVED***, bnode)
binding.addBinding(var, SimpleValueFactory.getInstance().createLiteral(value));
}

return binding;
} catch (Exception e) {
log.debug("Failed to parse binding: {}", e.getMessage());
return null;
}
}

/**
 * Extract variable names from SPARQL JSON head section.
 */
private Set<String> extractVariables(String jsonResponse) {
Set<String> vars = new LinkedHashSet<>();
try {
int headStart = jsonResponse.indexOf("\"head\"");
int varsStart = jsonResponse.indexOf("\"vars\"", headStart);
int arrayStart = jsonResponse.indexOf("[", varsStart);
int arrayEnd = jsonResponse.indexOf("]", arrayStart);

if (arrayStart > 0 && arrayEnd > arrayStart) {
String varsArray = jsonResponse.substring(arrayStart + 1, arrayEnd);
String[] varNames = varsArray.split(",");
for (String varName : varNames) {
varName = varName.trim().replaceAll("[\"\\s]", "");
if (!varName.isEmpty()) {
vars.add(varName);
}
}
}
} catch (Exception e) {
log.debug("Failed to extract variables: {}", e.getMessage());
}
return vars;
}

/**
 * Parse SPARQL JSON boolean result.
 * Format: { "head": {}, "boolean": true/false }
 */
private boolean parseSPARQLJsonBoolean(String jsonResponse) {
try {
int booleanIdx = jsonResponse.indexOf("\"boolean\"");
if (booleanIdx < 0) return false;

int colonIdx = jsonResponse.indexOf(":", booleanIdx);
String valueSection = jsonResponse.substring(colonIdx, Math.min(colonIdx + 20, jsonResponse.length()));

return valueSection.contains("true");
} catch (Exception e) {
log.debug("Failed to parse boolean result: {}", e.getMessage());
return false;
}
}

/**
 * Gets cached latency for an endpoint (from previous queries).
 */
public long getCachedLatency(String nodeId) {
return endpointLatencies.getOrDefault(nodeId, -1L);
}
}
