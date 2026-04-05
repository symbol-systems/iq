package systems.symbol.controller.rdf;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.rdf4j.fedx.FedXAPI;
import systems.symbol.rdf4j.fedx.I_FedXRepository;
import systems.symbol.rdf4j.fedx.StaticFedXTopology;
import systems.symbol.rdf4j.fedx.HTTPRemoteSPARQLClient;
import systems.symbol.rdf4j.fedx.SimpleFederatedQueryOptimizer;
import systems.symbol.control.node.I_NodeRegistry;
import java.net.InetAddress;

/**
 * REST endpoint for federated SPARQL queries.
 * 
 * Implements SPARQL Protocol for federated query execution across IQ cluster.
 * Base path: /sparql/federated
 *
 * Endpoints:
 *   GET  /sparql/federated?query=...  - SELECT or ASK queries
 *   POST /sparql/federated- SPARQL query with form-encoded body
 */
@Path("/sparql/federated")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class FedXResource {

private static final Logger log = LoggerFactory.getLogger(FedXResource.class);

@Inject
I_NodeRegistry nodeRegistry;

private FedXAPI fedxAPI;

/**
 * Initialize FedX API on first request using injected node registry.
 */
private void ensureFedXInitialized() {
if (fedxAPI == null) {
try {
log.info("Initializing FedX with injected node registry");

// Get local node ID from hostname
String localNodeId = getLocalNodeId();
log.debug("Using local node ID: {}", localNodeId);

// Create topology from control plane node registry
StaticFedXTopology topology = new StaticFedXTopology(nodeRegistry, localNodeId);

// Create remote SPARQL client
HTTPRemoteSPARQLClient client = new HTTPRemoteSPARQLClient();

// Create query optimizer
SimpleFederatedQueryOptimizer optimizer = new SimpleFederatedQueryOptimizer();

// Create high-level repository
systems.symbol.rdf4j.fedx.FedXRepository repository = 
new systems.symbol.rdf4j.fedx.FedXRepository(topology, client, optimizer);

// Wrap in REST API layer
fedxAPI = new FedXAPI(repository);

} catch (Exception e) {
log.error("Failed to initialize FedX", e);
throw new RuntimeException("FedX initialization failed", e);
}
}
}

/**
 * Get the local node ID from hostname (for Kubernetes: pod name).
 */
private String getLocalNodeId() {
try {
return InetAddress.getLocalHost().getHostName();
} catch (Exception e) {
log.warn("Failed to get hostname, using 'localhost'", e);
return "localhost";
}
}

/**
 * Execute SELECT or ASK query via GET.
 * Query must be URL-encoded in query parameter.
 *
 * @param query SPARQL query (SELECT or ASK)
 * @param timeout Optional query timeout in seconds
 * @return JSON SPARQL results
 */
@GET
@Path("/query")
public Response query(
@QueryParam("query") String query,
@QueryParam("timeout") Integer timeout) {

if (query == null || query.trim().isEmpty()) {
return Response.status(Response.Status.BAD_REQUEST)
.entity("{\"error\": \"query parameter required\"}")
.build();
}

try {
// Validate query and timeout
SPARQLQueryValidator.ValidationResult validation = 
SPARQLQueryValidator.validate(query, timeout);

if (!validation.isValid()) {
log.warn("Query validation failed: {}", validation.getMessage());
return Response.status(Response.Status.BAD_REQUEST)
.entity("{\"error\": \"" + escapeJSON(validation.getMessage()) + "\"}")
.build();
}

// Use validated timeout
Integer validatedTimeout = validation.getRecommendedTimeout();

ensureFedXInitialized();

String queryType = SPARQLQueryValidator.getQueryType(query);
String result;

try {
switch (queryType) {
case "SELECT":
log.debug("Executing federated SELECT query (timeout: {}s)", validatedTimeout);
result = fedxAPI.selectQuery(query, validatedTimeout);
break;
case "ASK":
log.debug("Executing federated ASK query (timeout: {}s)", validatedTimeout);
result = fedxAPI.askQuery(query, validatedTimeout);
break;
case "CONSTRUCT":
log.debug("Executing federated CONSTRUCT query (timeout: {}s)", validatedTimeout);
result = fedxAPI.constructQuery(query, validatedTimeout);
break;
default:
return Response.status(Response.Status.BAD_REQUEST)
.entity("{\"error\": \"Unknown query type: " + queryType + "\"}")
.build();
}
} catch (RepositoryException e) {
// Check if CONSTRUCT/DESCRIBE not yet implemented
if (e.getMessage() != null && e.getMessage().contains("not yet")) {
return Response.status(Response.Status.NOT_IMPLEMENTED)
.entity("{\"error\": \"" + escapeJSON(e.getMessage()) + "\"}")
.build();
}
throw e;
}

log.debug("Query executed successfully");
return Response.ok(result).build();

} catch (RepositoryException e) {
log.error("Query execution failed", e);
return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
.entity("{\"error\": \"" + escapeJSON(e.getMessage()) + "\"}")
.build();
} catch (Exception e) {
log.error("Unexpected error during query execution", e);
return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
.entity("{\"error\": \"Internal server error\"}")
.build();
}
}

/**
 * Execute query via POST with form-encoded body.
 *
 * @param query SPARQL query (SELECT, ASK, CONSTRUCT, or DESCRIBE)
 * @param timeout Optional query timeout in seconds
 * @return JSON SPARQL results
 */
@POST
@Path("/query")
public Response postQuery(
@FormParam("query") String query,
@FormParam("timeout") Integer timeout) {

return query(query, timeout);  // Delegate to GET handler
}

/**
 * Health check endpoint - verify FedX topology is available.
 *
 * @return Status with endpoint count
 */
@GET
@Path("/health")
public Response health() {
try {
ensureFedXInitialized();

I_FedXRepository repo = (I_FedXRepository) fedxAPI.getClass().getDeclaredField("repository").get(fedxAPI);
int endpointCount = repo.getTopology().getQueryableEndpoints().size();

return Response.ok("{\"status\": \"healthy\", \"endpoints\": " + endpointCount + "}")
.build();
} catch (Exception e) {
log.warn("Health check failed", e);
return Response.status(Response.Status.SERVICE_UNAVAILABLE)
.entity("{\"status\": \"unhealthy\", \"reason\": \"" + e.getMessage() + "\"}")
.build();
}
}

/**
 * Escape special characters for JSON output.
 */
private String escapeJSON(String value) {
if (value == null) return "";
return value
.replace("\\", "\\\\")
.replace("\"", "\\\"")
.replace("\n", "\\n")
.replace("\r", "\\r");
}
}
