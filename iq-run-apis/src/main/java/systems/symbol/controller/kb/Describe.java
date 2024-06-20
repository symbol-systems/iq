package systems.symbol.controller.kb;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.RDFResponse;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.string.Validate;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * RESTful endpoint for executing SPARQL describe on RDF subject.
 * The Queries are stored within the ScriptCatalog associated with the repository and context.
 */
@Path("describe")
public class Describe extends GuardedAPI  {

/**
 * Executes a SPARQL query on a specified RDF repository.
 *
 * @param repo   The name of the RDF repository.
 * @param iri   The iri to query.
 * @return Response containing the results of the SPARQL query in JSON format.
 */
@GET
@Path("{repo}/{iri: .*}")
@Produces("application/ld+json")
public Response describe(
@PathParam("repo") String repo,
@PathParam("iri") String iri,
@HeaderParam("Authorization") String auth) throws IOException {
if (!Validate.isBearer(auth)) {
log.info("describe#protected");
return new OopsResponse("api.describe#unauthorized", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.describe#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.describe#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
if (iri==null || iri.isEmpty()) iri = platform.getSelf().stringValue();
try (RepositoryConnection connection = repository.getConnection()) {
log.info("describe: {} -> {}", repo, iri);
GraphQuery query = connection.prepareGraphQuery("DESCRIBE <" + iri + ">");
RDFResponse rdfResponse = new RDFResponse(iri, query, RDFFormat.JSONLD);
return rdfResponse.asJSONLD();
} catch (URISyntaxException e) {
return new OopsResponse("api.describe#iri-invalid", Response.Status.BAD_REQUEST).asJSON();
}
}
}
