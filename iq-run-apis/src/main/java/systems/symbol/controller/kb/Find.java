package systems.symbol.controller.kb;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.RDFResponse;
import systems.symbol.finder.FactFinder;
import systems.symbol.platform.APIPlatform;
import systems.symbol.string.Validate;

@Path("find")
public class Find extends GuardedAPI {
DynamicModelFactory dmf = new DynamicModelFactory();

private final double DEFAULT_SCORE = 0.65;
@GET
@Path("{repo}/{finder}")
@Produces(MediaType.APPLICATION_JSON)
public Response importLocal(@PathParam("finder")String finder, @PathParam("repo")String repo,
@QueryParam("query") String query,
@QueryParam("score") double score, @QueryParam("max") int max,
@HeaderParam("Authorization") String auth) {
if (!Validate.isBearer(auth)) {
log.info("api.kb.find#protected");
if (!Validate.isUnGuarded())
return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.iq.find.indexer#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isNonAlphanumeric(finder)) {
return new OopsResponse("api.iq.find.indexer#finder-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isMissing(query)) {
return new OopsResponse("api.iq.find.indexer#query-missing", Response.Status.BAD_REQUEST).asJSON();
}
FactFinder factFinder = platform.getFactFinder(finder);
if (factFinder == null) {
return new OopsResponse("api.iq.find.indexer#finder-missing", Response.Status.NOT_FOUND).asJSON();
}
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.iq.find.indexer#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
//System.out.println("api.iq.find.indexer.repository: "+repository.isInitialized()+" @ "+repository.getDataDir().getAbsolutePath());
if (!repository.isInitialized()) {
return new OopsResponse("api.iq.find.indexer#repository-offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}

max = max==0?10:Math.min(Math.max(max, 1), 10); // clamp 1-10
score = score==0?DEFAULT_SCORE:Math.min(Math.max(score, 0.1), 1.0); // clamp 0.1-1.0

// retrieve results from SPARQL query to populate index
try (RepositoryConnection connection = repository.getConnection()) {
Model model = factFinder.search(dmf.createEmptyModel(), query, connection, max, score);
if (model.isEmpty() && score == DEFAULT_SCORE) {
log.info("api.kb.find#fallback");
model = factFinder.search(dmf.createEmptyModel(), query, connection, max, DEFAULT_SCORE/2);
}
return new RDFResponse(null, model, RDFFormat.JSONLD).asJSON();

} catch (Exception e) {
return new OopsResponse("api.iq.find.indexer#query-failed", e).asJSON();
}
}

}
