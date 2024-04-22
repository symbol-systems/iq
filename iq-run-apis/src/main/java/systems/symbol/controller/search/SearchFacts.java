package systems.symbol.controller.search;

import systems.symbol.platform.APIPlatform;
import systems.symbol.finder.FactFinder;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.string.Validate;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RESTful endpoint for searching facts in a knowledge base then hydrating the results from the lnowledge base.
 */
@Path("search/facts")
public class SearchFacts {

@Inject
APIPlatform platform;

/**
 * Searches for facts in a knowledge base using a specified text finder and SPARQL query.
 * Hydrates the found items using the specified SPARQL query and returns as JSON
 *
 * @param finder The name of the text finder.
 * @param repo   The name of the RDF repository.
 * @param sparql The name of the SPARQL query.
 * @param query  The search query.
 * @param maxResults The maximum number of results to retrieve.
 * @param relevancy  The relevancy threshold for search results.
 * @return Response containing the search results in JSON format.
 */
@GET
@Path("{finder}/{repo}/{sparql: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response search(
@PathParam("finder") String finder,
@PathParam("repo") String repo,
@PathParam("sparql") String sparql,
@QueryParam("query") String query,
@QueryParam("maxResults") int maxResults,
@QueryParam("relevancy") double relevancy
) {

// Initialize and perform sanity checks
if (Validate.isNonAlphanumeric(finder)) {
return new OopsResponse("api.search.facts#finder-invalid", Response.Status.BAD_REQUEST).asJSON();
}

// Get the text finder instance from the platform
FactFinder searcher = platform.getFactFinder(finder);
if (searcher == null) {
return new OopsResponse("api.search.facts#finder-missing", Response.Status.NOT_FOUND).asJSON();
}

// Get the RDF repository instance from the platform
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.search.facts#repository-missing", Response.Status.NOT_FOUND).asJSON();
}

// Check if the repository is initialized
if (!repository.isInitialized()) {
return new OopsResponse("api.search.facts#repository.offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}

// Use the platform SPARQL repository
try (RepositoryConnection connection = repository.getConnection()) {
IQConnection iq = new IQConnection(platform.getSelf(), connection);
IQScriptCatalog library = new IQScriptCatalog(iq);

// Set a default relevancy threshold if not provided
if (relevancy < 0.1) relevancy = 0.5;

// Find matches using the text finder
List<EmbeddingMatch<TextSegment>> matches = searcher.find(query, maxResults, relevancy);

// Convert matches to a VALUES clause for SPARQL query
StringBuilder theseMatches = new StringBuilder();
for (int i = 0; i < matches.size(); i++) {
theseMatches.append("(")
.append("<").append(matches.get(i).embeddingId()).append("> ")
.append(matches.get(i).score())
.append(")");
}
Map<String, Object> bindings = new HashMap<>();
bindings.put("these", theseMatches.toString());

// Use a query to hydrate answers
// The query is interpolated to respect {{these}} VALUES bindings
String hydrateQuery = library.getSPARQL(sparql, bindings);
if (hydrateQuery == null || hydrateQuery.isEmpty()) {
return new OopsResponse("api.search.facts#query-missing", Response.Status.NOT_FOUND).asJSON();
}

System.out.println("api.search.facts.hydrate: " + hydrateQuery);

// Execute SPARQL query against the repository
TupleQuery tupleQuery = connection.prepareTupleQuery(hydrateQuery);
try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
List<Map<String, Object>> models = SPARQLMapper.toMaps(evaluate);
return new SimpleResponse(models).asJSON();
} catch (QueryEvaluationException e) {
return new OopsResponse("api.search.facts#query-failed", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}
} catch (IOException e) {
return new OopsResponse("api.search.facts#failed", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}
}
}
