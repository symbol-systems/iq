package systems.symbol.controller.callback;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.string.Validate;

import java.io.IOException;

/**
 * RESTful endpoint for checking the event status of the platform and RDF repositories.
 */
@Path("event")
public class Event extends GuardedAPI {

/**
 * Receives an event callback from a source, saves to  repository.
 *
 * @param repo The name of the repository.
 * @param source The source of the event.
 * @return response indicating the repository's event status.
 */
@Path("{repo}/{source: .*}")
@POST
@Produces(MediaType.APPLICATION_JSON)
public Response repositoryHealth(@PathParam("repo") String repo, @PathParam("source") String source,
@HeaderParam("Authorization") String auth) throws IOException {
if (!Validate.isBearer(auth)) {
log.info("api.event#protected-repository");
return new OopsResponse("api.event#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.event#repository-missing", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isNonAlphanumeric(source)) {
return new OopsResponse("api.event#source-missing", Response.Status.BAD_REQUEST).asJSON();
}
//Repository repository = this.platform.getRepository(repo);
return new HealthCheck("api.event#todo").asJSON();
}
}
