package systems.symbol.controller.kb;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.RDFResponse;
import systems.symbol.rdf4j.iq.IQConnection;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.string.Validate;

import java.net.URISyntaxException;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the repository and context.
 */
@Path("construct")
public class Construct extends GuardedAPI {
//protected final Logger log = LoggerFactory.getLogger(getClass());


/**
 * Executes a SPARQL construct query on a specified RDF repository.
 *
 * @param repo   The name of the RDF repository.
 * @param query   The query of a SPARQL construct query.
 * @return Response containing the results of the SPARQL query in JSON format.
 */
@GET
@Path("{repo}/{query: .*}")
@Produces("application/ld+json")
public Response constructQuery(@PathParam("repo") String repo,
   @PathParam("query") String query,
   @HeaderParam("Authorization") String auth) {
if (!Validate.isBearer(auth)) {
log.info("api.construct#protected");
if (!Validate.isUnGuarded())
return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.construct#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isMissing(query)) {
return new OopsResponse("api.construct#query-invalid", Response.Status.BAD_REQUEST).asJSON();
}

Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.construct#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
try (RepositoryConnection connection = repository.getConnection()) {

IQConnection iq = new IQConnection(platform.getSelf(), connection);
ScriptCatalog catalog = new ScriptCatalog(iq);
String sparql = catalog.getSPARQL(query);

log.info("construct: {} -> {} --> {}", repo, query, sparql);
GraphQuery graphQuery = connection.prepareGraphQuery(sparql);

RDFResponse rdfResponse = new RDFResponse(iq.getSelf().stringValue(), graphQuery, RDFFormat.JSONLD);
return rdfResponse.asJSONLD();
} catch (URISyntaxException e) {
return new OopsResponse("api.construct#query-invalid", Response.Status.BAD_REQUEST).asJSON();
}
}
}
