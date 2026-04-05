package systems.symbol.rdf4j.fedx;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;
import java.util.stream.Collectors;

/**
 * Simple federated query optimizer.
 * Provides basic endpoint selection and filter pushdown.
 *
 * Features:
 * - Estimates which endpoints can answer which triple patterns
 * - Prioritizes fast endpoints (based on cached latency)
 * - Pushes down FILTER expressions to remote endpoints
 * - Avoids querying the local node (federation between remotes)
 *
 * Future: Implement cardinality-based optimization with statistics.
 */
public class SimpleFederatedQueryOptimizer implements I_FederatedQueryOptimizer {

private static final Logger log = LoggerFactory.getLogger(SimpleFederatedQueryOptimizer.class);

private static final Pattern PREFIX_PATTERN = Pattern.compile(
"PREFIX\\s+\\S+\\s+<[^>]+>", Pattern.CASE_INSENSITIVE);

private static final Pattern TRIPLE_PATTERN = Pattern.compile(
"([?$]\\w+|<[^>]+>|\\w+:\\w*)\\s+([?$]\\w+|<[^>]+>|\\w+:\\w*|a)\\s+([?$]\\w+|<[^>]+>|\\w+:\\w*|\"[^\"]*\"(?:@\\w+|\\^\\^\\S+)?)\\s*[.;]");

private static final Pattern FILTER_PATTERN = Pattern.compile(
"FILTER\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);

private static final Pattern WHERE_BODY_PATTERN = Pattern.compile(
"WHERE\\s*\\{(.+)\\}\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

private static final Pattern QUERY_PROLOGUE_PATTERN = Pattern.compile(
"^(.*?WHERE\\s*\\{)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

@Override
public String optimizeQueryForFederation(String sparqlQuery, I_FedXTopology topology,
I_RemoteSPARQLClient client) {
if (topology.isEmpty()) {
log.warn("Federated query optimization: empty topology");
return sparqlQuery;
}

log.debug("Optimizing query for federation with {} endpoints", topology.size());

// Step 1: Analyze query structure — extract triple patterns and filters
QueryStructure structure = analyzeQueryStructure(sparqlQuery);
if (structure.triplePatterns.isEmpty()) {
log.debug("No triple patterns found in query; returning original");
return sparqlQuery;
}
log.debug("Analyzed query: {} triple patterns, {} filters",
structure.triplePatterns.size(), structure.filters.size());

// Step 2: Estimate selectivity — probe endpoints with ASK queries per triple pattern
Map<String, List<FedXEndpoint>> patternEndpoints = estimateSelectivity(
structure.triplePatterns, structure.prefixes, topology, client);

// Step 3: Determine endpoint selection — sort by latency, exclude local node
Map<String, List<FedXEndpoint>> rankedEndpoints = rankEndpoints(patternEndpoints, topology, client);
if (rankedEndpoints.values().stream().allMatch(List::isEmpty)) {
log.debug("No remote endpoints matched any triple pattern; returning original query");
return sparqlQuery;
}

// Step 4: Push filters down — rewrite query with SERVICE clauses and embedded filters
String optimized = rewriteWithServiceClauses(sparqlQuery, structure, rankedEndpoints);
log.debug("Optimized query:\n{}", optimized);
return optimized;
}

@Override
public String getOptimizerName() {
return "SimpleFederatedQueryOptimizer";
}

// ── Step 1: Analyze query structure ──────────────────────────────────────────

QueryStructure analyzeQueryStructure(String sparqlQuery) {
QueryStructure qs = new QueryStructure();

// Extract PREFIX declarations
Matcher prefixMatcher = PREFIX_PATTERN.matcher(sparqlQuery);
while (prefixMatcher.find()) {
qs.prefixes.add(prefixMatcher.group());
}

// Extract the WHERE body
Matcher bodyMatcher = WHERE_BODY_PATTERN.matcher(sparqlQuery);
if (!bodyMatcher.find()) {
return qs;
}
String whereBody = bodyMatcher.group(1);

// Extract FILTER expressions
Matcher filterMatcher = FILTER_PATTERN.matcher(whereBody);
while (filterMatcher.find()) {
qs.filters.add(filterMatcher.group(0)); // full "FILTER(...)"
}

// Remove FILTER blocks so they don't interfere with triple pattern matching
String patternsOnly = FILTER_PATTERN.matcher(whereBody).replaceAll("");

// Extract triple patterns
Matcher tripleMatcher = TRIPLE_PATTERN.matcher(patternsOnly);
while (tripleMatcher.find()) {
String tp = tripleMatcher.group(1) + " " + tripleMatcher.group(2) + " " + tripleMatcher.group(3);
qs.triplePatterns.add(tp.trim());
}

return qs;
}

// ── Step 2: Estimate selectivity ─────────────────────────────────────────────

Map<String, List<FedXEndpoint>> estimateSelectivity(
List<String> triplePatterns, List<String> prefixes,
I_FedXTopology topology, I_RemoteSPARQLClient client) {

Collection<FedXEndpoint> queryable = topology.getQueryableEndpoints();
Optional<FedXEndpoint> local = topology.getLocalEndpoint();
String prefixBlock = String.join("\n", prefixes);

Map<String, List<FedXEndpoint>> result = new LinkedHashMap<>();

for (String tp : triplePatterns) {
List<FedXEndpoint> matching = new ArrayList<>();
String askQuery = prefixBlock + "\nASK WHERE { " + tp + " }";

for (FedXEndpoint ep : queryable) {
// Skip the local node — federation is between remotes
if (local.isPresent() && ep.nodeId().equals(local.get().nodeId())) {
continue;
}
try {
boolean hasData = client.askQuery(ep, askQuery, QueryLanguage.SPARQL);
if (hasData) {
matching.add(ep);
}
} catch (Exception e) {
log.debug("ASK probe failed on {} for pattern [{}]: {}",
ep.nodeId(), tp, e.getMessage());
}
}
result.put(tp, matching);
}
return result;
}

// ── Step 3: Determine endpoint selection (rank by latency) ───────────────────

Map<String, List<FedXEndpoint>> rankEndpoints(
Map<String, List<FedXEndpoint>> patternEndpoints,
I_FedXTopology topology, I_RemoteSPARQLClient client) {

// Collect latency for every endpoint that appeared in at least one pattern
Map<String, Long> latencyCache = new HashMap<>();
for (List<FedXEndpoint> eps : patternEndpoints.values()) {
for (FedXEndpoint ep : eps) {
latencyCache.computeIfAbsent(ep.nodeId(), id -> client.getEndpointLatency(ep));
}
}

Map<String, List<FedXEndpoint>> ranked = new LinkedHashMap<>();
for (Map.Entry<String, List<FedXEndpoint>> entry : patternEndpoints.entrySet()) {
List<FedXEndpoint> sorted = entry.getValue().stream()
.filter(ep -> {
long latency = latencyCache.getOrDefault(ep.nodeId(), -1L);
return latency >= 0; // exclude unreachable endpoints
})
.sorted(Comparator.comparingLong(
ep -> latencyCache.getOrDefault(ep.nodeId(), Long.MAX_VALUE)))
.collect(Collectors.toList());
ranked.put(entry.getKey(), sorted);
}
return ranked;
}

// ── Step 4: Push filters down — rewrite using SERVICE clauses ────────────────

String rewriteWithServiceClauses(String sparqlQuery, QueryStructure structure,
 Map<String, List<FedXEndpoint>> rankedEndpoints) {

// Extract the prologue (everything up to and including "WHERE {")
Matcher prologueMatcher = QUERY_PROLOGUE_PATTERN.matcher(sparqlQuery);
if (!prologueMatcher.find()) {
return sparqlQuery;
}
String prologue = prologueMatcher.group(1);

// Determine which filters reference which variables
Map<String, Set<String>> filterVars = new LinkedHashMap<>();
for (String filter : structure.filters) {
Set<String> vars = extractVariables(filter);
filterVars.put(filter, vars);
}

// Build SERVICE blocks — group consecutive patterns targeting the same endpoint
StringBuilder body = new StringBuilder();
Set<String> usedFilters = new HashSet<>();

for (Map.Entry<String, List<FedXEndpoint>> entry : rankedEndpoints.entrySet()) {
String triplePattern = entry.getKey();
List<FedXEndpoint> endpoints = entry.getValue();

// Find filters whose variables are all present in this triple pattern
Set<String> patternVars = extractVariables(triplePattern);
List<String> applicableFilters = new ArrayList<>();
for (Map.Entry<String, Set<String>> fv : filterVars.entrySet()) {
if (patternVars.containsAll(fv.getValue()) && !usedFilters.contains(fv.getKey())) {
applicableFilters.add(fv.getKey());
usedFilters.add(fv.getKey());
}
}

String filterBlock = applicableFilters.isEmpty() ? ""
: "\n" + String.join("\n", applicableFilters);

if (endpoints.isEmpty()) {
// No remote endpoint matched — keep the pattern local (no SERVICE wrapper)
body.append("  ").append(triplePattern).append(" .\n");
if (!filterBlock.isEmpty()) {
body.append("  ").append(filterBlock.trim()).append("\n");
}
} else {
// Use the fastest (first) endpoint for execution
FedXEndpoint target = endpoints.get(0);
body.append("  SERVICE <").append(target.sparqlEndpoint()).append("> {\n");
body.append("").append(triplePattern).append(" .");
if (!filterBlock.isEmpty()) {
body.append(filterBlock);
}
body.append("\n  }\n");
}
}

// Append any remaining filters that were not pushed down
for (String filter : structure.filters) {
if (!usedFilters.contains(filter)) {
body.append("  ").append(filter).append("\n");
}
}

// Extract optional trailing modifiers (ORDER BY, LIMIT, OFFSET, GROUP BY, HAVING)
String trailingModifiers = extractTrailingModifiers(sparqlQuery);

return prologue + "\n" + body + "}" + trailingModifiers;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private Set<String> extractVariables(String fragment) {
Set<String> vars = new LinkedHashSet<>();
Matcher m = Pattern.compile("[?$](\\w+)").matcher(fragment);
while (m.find()) {
vars.add("?" + m.group(1));
}
return vars;
}

private String extractTrailingModifiers(String sparqlQuery) {
// Find everything after the last closing brace of the WHERE block
int lastBrace = sparqlQuery.lastIndexOf('}');
if (lastBrace < 0 || lastBrace >= sparqlQuery.length() - 1) {
return "";
}
String tail = sparqlQuery.substring(lastBrace + 1).trim();
return tail.isEmpty() ? "" : "\n" + tail;
}

// ── Inner types ──────────────────────────────────────────────────────────────

static class QueryStructure {
final List<String> prefixes = new ArrayList<>();
final List<String> triplePatterns = new ArrayList<>();
final List<String> filters = new ArrayList<>();
}
}
