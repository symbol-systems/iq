package systems.symbol.controller.rdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.control.node.ClusterNode;
import systems.symbol.control.node.ClusterNodeState;
import systems.symbol.control.node.I_NodeRegistry;

import java.util.*;
import java.util.***REMOVED***.Pattern;
import java.util.stream.Collectors;

/**
 * Optimizes federated SPARQL query execution across cluster nodes.
 *
 * Strategies:
 * - Query decomposition: Break complex queries into sub-queries for parallel execution
 * - Graph partitioning awareness: Route queries to nodes containing relevant data
 * - Result streaming: Support incremental result collection
 * - LIMIT/OFFSET optimization: Push down to remote endpoints when safe
 */
public class DistributedQueryOptimizer {

private static final Logger log = LoggerFactory.getLogger(DistributedQueryOptimizer.class);

private final I_NodeRegistry nodeRegistry;

// Pattern matching for OPTIONAL and UNION graph patterns
private static final Pattern OPTIONAL_PATTERN = Pattern.compile(
"OPTIONAL\\s*\\{[^}]*\\}",
Pattern.CASE_INSENSITIVE | Pattern.DOTALL
);

private static final Pattern UNION_PATTERN = Pattern.compile(
"\\}\\s*UNION\\s*\\{",
Pattern.CASE_INSENSITIVE | Pattern.DOTALL
);

private static final Pattern LIMIT_PATTERN = Pattern.compile(
"LIMIT\\s+(\\d+)",
Pattern.CASE_INSENSITIVE
);

private static final Pattern OFFSET_PATTERN = Pattern.compile(
"OFFSET\\s+(\\d+)",
Pattern.CASE_INSENSITIVE
);

public DistributedQueryOptimizer(I_NodeRegistry nodeRegistry) {
this.nodeRegistry = Objects.requireNonNull(nodeRegistry, "nodeRegistry");
}

/**
 * Analyzes a query and produces optimization metadata.
 *
 * @param query SPARQL query to analyze
 * @return QueryOptimizationPlan with execution strategies
 */
public QueryOptimizationPlan analyze(String query) {
Objects.requireNonNull(query, "query");

log.debug("Analyzing query for distributed execution");

QueryOptimizationPlan plan = new QueryOptimizationPlan();

// Detect query characteristics
plan.canParallelize = detectParallelizability(query);
plan.hasOptional = OPTIONAL_PATTERN.matcher(query).find();
plan.hasUnion = UNION_PATTERN.matcher(query).find();
plan.complexity = calculateComplexity(query);

// Determine execution strategy
plan.executionStrategy = selectStrategy(query, plan);

// Get available nodes for distribution
List<ClusterNode> healthyNodes = nodeRegistry.listByState(ClusterNodeState.HEALTHY)
.stream()
.sorted(Comparator.comparing(ClusterNode::nodeId))
.collect(Collectors.toList());

plan.targetNodes = selectTargetNodes(healthyNodes, plan);

log.debug("Query optimization plan: strategy={}, parallelizable={}, complexity={}, targets={}", 
plan.executionStrategy, plan.canParallelize, plan.complexity, plan.targetNodes.size());

return plan;
}

/**
 * Detects if a query can be safely executed in parallel across nodes.
 * 
 * Queries with OPTIONAL and UNION patterns can often be executed in parallel.
 * Queries with complex JOINs may need sequential execution.
 */
private boolean detectParallelizability(String query) {
String upper = query.toUpperCase();

// Can parallelize if:
// - Has UNION (each branch is independent)
// - Has OPTIONAL (branches can be queried separately)

// Cannot parallelize if:
// - Complex nested JOINs (multiple connected patterns)

boolean hasUnion = UNION_PATTERN.matcher(query).find();
boolean hasOptional = OPTIONAL_PATTERN.matcher(query).find();
int tripleCount = countGraphPatterns(query);

// Simple queries with 1-2 patterns can parallelize with BROADCAST
if (tripleCount <= 2) {
return true;
}

return (hasUnion || hasOptional);
}

/**
 * Calculates query complexity (low=simple, medium=moderate, high=complex).
 */
private String calculateComplexity(String query) {
int patterns = countGraphPatterns(query);
int filters = countFilters(query);
int joins = countJoins(query);

// Simplified scoring based on query structure
if (patterns <= 2 && filters == 0 && joins <= 1) {
return "LOW";
}

if (patterns <= 5 && filters <= 2) {
return "MEDIUM";
}

return "HIGH";
}

/**
 * Selects execution strategy based on query characteristics.
 */
private String selectStrategy(String query, QueryOptimizationPlan plan) {
if (!plan.canParallelize) {
return "SEQUENTIAL";  // Execute on single node
}

// Check structural properties first (UNION, OPTIONAL)
if (plan.hasUnion) {
return "UNION_DISTRIBUTE";  // Decompose UNION branches, execute in parallel
}

if (plan.hasOptional) {
return "OPTIONAL_DISTRIBUTE";  // Execute optional branches in parallel
}

// Then check complexity
if (plan.complexity.equals("LOW")) {
return "BROADCAST";  // Simple query, can send to all nodes
}

// Default to FedX federation for anything with parallelizability
return "FEDERATED";  // Use FedX's built-in federation strategy
}

/**
 * Selects which cluster nodes to target for execution.
 */
private List<ClusterNode> selectTargetNodes(List<ClusterNode> allNodes, QueryOptimizationPlan plan) {
if (allNodes.isEmpty()) {
log.warn("No healthy nodes available for query execution");
return Collections.emptyList();
}

if ("SEQUENTIAL".equals(plan.executionStrategy)) {
// Use only the first healthy node
return allNodes.stream().limit(1).collect(Collectors.toList());
}

if ("BROADCAST".equals(plan.executionStrategy)) {
// Use all available nodes
return new ArrayList<>(allNodes);
}

// For UNION and OPTIONAL strategies, distribute across available nodes
// Try to select diverse nodes (different hosts if possible)
return selectDiverseNodes(allNodes, Math.min(allNodes.size(), 3));
}

/**
 * Selects diverse nodes (geographically or by host)  for better parallelism.
 */
private List<ClusterNode> selectDiverseNodes(List<ClusterNode> nodes, int maxCount) {
if (nodes.size() <= maxCount) {
return new ArrayList<>(nodes);
}

// Simple selection: first, middle, last
List<ClusterNode> diverse = new ArrayList<>();
diverse.add(nodes.get(0));

if (maxCount > 1) {
diverse.add(nodes.get(nodes.size() / 2));
}

if (maxCount > 2) {
diverse.add(nodes.get(nodes.size() - 1));
}

return diverse;
}

private int countGraphPatterns(String query) {
// Count triple patterns by looking for dots followed by closing braces or other patterns
String wherePart = query.toUpperCase().contains("WHERE") ?
query.substring(query.toUpperCase().indexOf("WHERE")) : query;

// Simple heuristic: count dots that are likely triple pattern endings
int count = 0;
for (char c : wherePart.toCharArray()) {
if (c == '.') count++;
}
return Math.max(1, count);  // At least 1 pattern
}

private int countFilters(String query) {
return (int) query.toUpperCase().split("FILTER").length - 1;
}

private int countJoins(String query) {
// Count unique variables as a proxy for join complexity
// More variables often means more complex joins
String vars = query.replaceAll("[^?a-zA-Z0-9]", " ");
int varCount = (int) vars.split("\\?").length - 1;
return Math.max(0, varCount / 2);  // Approximate joins from variable count
}

/**
 * Query optimization plan with execution strategy.
 */
public static class QueryOptimizationPlan {
public boolean canParallelize;
public boolean hasOptional;
public boolean hasUnion;
public String complexity;  // LOW, MEDIUM, HIGH
public String executionStrategy;  // SEQUENTIAL, BROADCAST, UNION_DISTRIBUTE, etc.
public List<ClusterNode> targetNodes;  // Nodes to execute on

public QueryOptimizationPlan() {
this.targetNodes = new ArrayList<>();
}

public boolean shouldParallelize() {
return canParallelize && targetNodes.size() > 1;
}

public int estimatedParallelGain() {
if (!shouldParallelize()) return 1;
// Estimate speedup based on node count and complexity
return Math.min(targetNodes.size(), 4);  // Cap at 4x due to coordination overhead
}
}
}
