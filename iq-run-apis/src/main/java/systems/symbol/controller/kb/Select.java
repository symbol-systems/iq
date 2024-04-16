package systems.symbol.controller.kb;

import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.rdf4j.iq.IQConnection;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.string.Validate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.List;
import java.util.Map;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the repository and context.
 */
@Path("select")
public class Select extends GuardedAPI {
/**
 * Executes a SPARQL query on a specified RDF repository.
 *
 * @param repo   The name of the RDF repository.
 * @param query  The SPARQL query.
 * @param maxResults The maximum number of results to retrieve.
 * @return Response containing the results of the SPARQL query in JSON format.
 */
@GET
@Path("{repo}/{query: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response query(
@PathParam("repo") String repo,
@PathParam("query") String query,
@QueryParam("maxResults") int maxResults,
@HeaderParam("Authorization") String auth) {
if (!Validate.isBearer(auth)) {
log.info("api.kb.sparql#protected");
if (!Validate.isUnGuarded())
return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.kb.sparql#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isMissing(query)) {
return new OopsResponse("api.kb.sparql#query-invalid", Response.Status.BAD_REQUEST).asJSON();
}

if (maxResults<0) maxResults = 10000;
// Get the repository instance from the platform
Repository repository = platform.getRepository(repo);
if (repository == null) {
// Return an error response if the repository is not found
return new OopsResponse("api.kb.sparql#repository-missing", Response.Status.NOT_FOUND).asJSON();
}

try (RepositoryConnection connection = repository.getConnection()) {
// Lookup SPARQL query in the platform repository
IQConnection iq = new IQConnection(platform.getSelf(), connection);
ScriptCatalog library = new ScriptCatalog(iq);
String sparql = library.getSPARQL(query + ".sparql");

// Check for non-null and non-empty SPARQL query
if (sparql == null || sparql.isEmpty()) {
return new OopsResponse("api.kb.sparql#query-missing", Response.Status.NOT_FOUND).asJSON();
}

// Execute SPARQL query against the repository
TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);

try (TupleQueryResult result = tupleQuery.evaluate()) {
// Convert SPARQL results to a list of maps
List<Map<String, Object>> models = SPARQLMapper.toMaps(result);
return new SimpleResponse(models).asJSON();
} catch (QueryEvaluationException e) {
// Return an error response if SPARQL query execution fails
return new OopsResponse("api.kb.sparql#query-failed", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}
}
}
}
