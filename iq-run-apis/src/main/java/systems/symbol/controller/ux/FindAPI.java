package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.LDResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.I_Found;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyString;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.SimpleBindings;

/**
 * RESTful endpoint for searching facts in a knowledge base then hydrating the
 * results from the knowledge base.
 */
@Path("ux/find")
@Tag(name = "api.ux.find.name", description = "api.ux.find.description")
public class FindAPI extends GuardedAPI {
@ConfigProperty(name = "iq.realm.find.minScore", defaultValue = "0.1")
double minScore;
@ConfigProperty(name = "iq.realm.find.maxResults", defaultValue = "10")
int maxResults;

/**
 * Searches for facts in a knowledge base using a specified text finder and
 * SPARQL query.
 * Hydrates the found items using the specified SPARQL query and returns as JSON
 *
 * @param repo   The name of the text finder.
 * @param model  The name of the SPARQL query.
 * @param query  The search query.
 * @param maxResults The maximum number of results to retrieve.
 * @param relevancy  The relevancy threshold for search results.
 * @return Response containing the search results in JSON format.
 */
@GET
@Operation(summary = "api.ux.find.get.summary", description = "api.ux.find.get.description")
@Path("{repo}/{model: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response search(
@PathParam("repo") String repo,
@PathParam("model") String model,
@QueryParam("query") String query,
@QueryParam("maxResults") int maxResults,
@QueryParam("relevancy") double relevancy,
@Context UriInfo uriInfo,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {
return doSearch(repo, model, query, maxResults, relevancy, uriInfo, auth);
}

@POST
@Operation(summary = "api.ux.find.post.summary", description = "api.ux.find.post.description")
@Path("{repo}/{model: .*}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public Response searchModel(
@PathParam("repo") String repo,
@PathParam("model") String model,
SimpleBindings body,
@Context UriInfo uriInfo,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {

String query = (String) body.get("query");
Integer maxResults = PrettyString.get(body, "maxResults", this.maxResults);
double relevancy = PrettyString.get(body, "relevancy", this.minScore);
return doSearch(repo, model, query, maxResults, relevancy, uriInfo, auth);
}

public Response doSearch(
String repo,
String model,
String query,
int maxResults,
double relevancy,
UriInfo uriInfo,
String auth) throws SecretsException {

Stopwatch stopwatch = new Stopwatch();
log.info("ux.find: {} --> {} -> {}", repo, query, uriInfo.getQueryParameters().keySet());

if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("ux.find.repository", Response.Status.BAD_REQUEST).build();
}
I_Realm realm = platform.getRealm(repo);
if (realm == null)
return new OopsResponse("ux.find.realm", Response.Status.NOT_FOUND).build();
DecodedJWT jwt;
try {
jwt = authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}

if (!Validate.isURN(model)) {
model = model.isEmpty() ? realm.getSelf() + "ux/find" : realm.getSelf() + model;
}
log.info("ux.find.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());
FactFinder searcher = realm.getFinder();
if (searcher == null) {
return new OopsResponse("ux.find.finder-missing", Response.Status.NOT_FOUND).build();
}
Repository repository = realm.getRepository();
if (repository == null) {
return new OopsResponse("ux.find.repository", Response.Status.NOT_FOUND).build();
}
if (!repository.isInitialized()) {
return new OopsResponse("ux.find.repository.offline", Response.Status.SERVICE_UNAVAILABLE).build();
}
if (relevancy < 0.001)
relevancy = 0.1;
if (maxResults < 1 || maxResults > 100) {
maxResults = 10;
}

log.info("ux.find.start: {} --> {} > {}", stopwatch.summary(), maxResults, relevancy);
try (RepositoryConnection connection = repository.getConnection()) {

Collection<I_Found<IRI>> found = realm.byConcept(realm.getSelf()).search(query, maxResults, relevancy);
StringBuilder matched = new StringBuilder();
// Convert matches to a VALUES clause for SPARQL query
for (I_Found<IRI> match : found) {
matched.append("(")
.append("<").append(match.intent().stringValue()).append("> ")
.append(match.score())
.append(")");
}
Map<String, Object> bindings = new HashMap<>();
bindings.put("these", matched.toString());
log.info("ux.find.found: {}", matched.toString().replaceAll("<|>", ""));

IQScriptCatalog scripts = new IQScriptCatalog(realm.getSelf(), connection);
// The query is interpolated using {{these}} bindings
// VALUES (?this ?score) { {{{these}}} }
String sparql = scripts.getSPARQL(model, bindings);
log.info("ux.find.hydrate: {} -> {} @ {}", model, sparql.replaceAll("<|>", ""), stopwatch.summary());
if (sparql == null || sparql.isEmpty()) {
return new OopsResponse("ux.find.hydrate", Response.Status.NOT_FOUND).build();
}
if (sparql.toUpperCase().contains("CONSTRUCT"))
return graphQuery(connection, sparql, stopwatch);
else
return tupleQuery(connection, sparql, stopwatch);

} catch (IOException e) {
return new OopsResponse("ux.find.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
} catch (StateException e) {
return new OopsResponse("ux.find.state", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

Response tupleQuery(RepositoryConnection connection, String sparql, Stopwatch stopwatch) {
// Use select query to synthesize an answer model
TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);
try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
List<Map<String, Object>> models = SPARQLMapper.toMaps(evaluate);
log.info("ux.find.done: {} @ {}", models.size(), stopwatch.summary());
DataResponse response = new DataResponse(models);
response.set("found", models.size());
response.set("elapsed", stopwatch.elapsed());
return response.build();
} catch (QueryEvaluationException e) {
return new OopsResponse("ux.find.tuple", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

Response graphQuery(RepositoryConnection connection, String sparql, Stopwatch stopwatch) {
// Use construct query to synthesize an answer model
GraphQuery query = connection.prepareGraphQuery(sparql);
try {
LDResponse response = new LDResponse(query);
return response.build();
} catch (Exception e) {
return new OopsResponse("ux.find.graph", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}
}
