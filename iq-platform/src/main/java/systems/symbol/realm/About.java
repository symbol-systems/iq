package systems.symbol.realm;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analytics for the IQ realm.
 * Provides metrics about the graph, agents, and platform state.
 */
public final class About {
private static final Logger log = LoggerFactory.getLogger(About.class);

private About() {
// Utility class
}

/**
 * Compute Phi metric: normalized graph density and health.
 * Phi = (triple_count * graph_count * agent_count / 1000000)
 *
 * @param repository The realm's Repository
 * @return Phi value (higher = more active realm)
 */
public static double computePhi(Repository repository) {
try (RepositoryConnection conn = repository.getConnection()) {
// Query 1: Count total triples in realm
long tripleCount = countTriples(conn);

// Query 2: Count named graphs 
long graphCount = countGraphs(conn);

// Query 3: Count agents/actors
long agentCount = countAgents(conn);

// Compute Phi
double phi = (tripleCount * graphCount * Math.max(1, agentCount)) / 1000000.0;

log.debug("Phi computation: triples={}, graphs={}, agents={}, phi={}", 
 tripleCount, graphCount, agentCount, phi);

return Math.max(1.0, phi);  // Minimum phi of 1.0
} catch (Exception e) {
log.warn("Error computing Phi, returning default: {}", e.getMessage());
return 1.0;
}
}

/**
 * Compute normalized Phi: Phi divided by expected baseline.
 * Shows how active the realm is relative to a "normal" realm.
 *
 * @param repository The realm's Repository
 * @return Normalized Phi (1.0 = baseline, >1.0 = very active)
 */
public static double computePhiNormal(Repository repository) {
try {
double phi = computePhi(repository);
// Baseline: realm with 100K triples, 50 graphs, 10 agents
double basePhi = (100000.0 * 50.0 * 10.0) / 1000000.0;
return phi / Math.max(1.0, basePhi);
} catch (Exception e) {
log.warn("Error computing Phi Normal, returning default: {}", e.getMessage());
return 1.0;
}
}

/**
 * Count total triples in realm using SPARQL.
 */
private static long countTriples(RepositoryConnection conn) throws Exception {
String query = "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }";
try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
if (result.hasNext()) {
BindingSet binding = result.next();
Number count = (Number) binding.getBinding("count").getValue();
return count.longValue();
}
}
return 0;
}

/**
 * Count named graphs in repository.
 */
private static long countGraphs(RepositoryConnection conn) throws Exception {
String query = "SELECT (COUNT(DISTINCT ?g) as ?count) WHERE { ?s ?p ?o GRAPH ?g { ?s ?p ?o } }";
try {
TupleQueryResult result = conn.prepareTupleQuery(query).evaluate();
try {
if (result.hasNext()) {
BindingSet binding = result.next();
Number count = (Number) binding.getBinding("count").getValue();
return count.longValue();
}
} finally {
result.close();
}
} catch (Exception e) {
// Fallback if GRAPH query fails
log.debug("Named graph query failed, returning 0: {}", e.getMessage());
return 0;
}
return 0;
}

/**
 * Count agents/actors in realm.
 * Looks for instances of agent/actor types.
 */
private static long countAgents(RepositoryConnection conn) throws Exception {
String query = "SELECT (COUNT(?a) as ?count) WHERE { " +
  "?a a ?type FILTER(?type IN(iq:Agent, iq:Actor, iq:Agentic)) }";
try {
TupleQueryResult result = conn.prepareTupleQuery(query).evaluate();
try {
if (result.hasNext()) {
BindingSet binding = result.next();
Number count = (Number) binding.getBinding("count").getValue();
return count.longValue();
}
} finally {
result.close();
}
} catch (Exception e) {
// Fallback if agent count fails
log.debug("Agent count query failed, returning 0: {}", e.getMessage());
return 0;
}
return 0;
}
}
