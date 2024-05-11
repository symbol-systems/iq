package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.Repository;
import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.string.Validate;

import java.io.IOException;

/**
 * RESTful endpoint for checking the health status of the platform and RDF repositories.
 */
@Path("health")
public class Healthy extends GuardedAPI{

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
@GET
@Produces(MediaType.APPLICATION_JSON)
public HealthCheck platformHealth() {
return new HealthCheck(platform.isHealthy() ? "ok" : "api.offline");

}

/**
 * Checks the health status of a specific repository within the platform.
 *
 * @param repo The name of the repository to check.
 * @return HealthCheck response indicating the repository's health status.
 */
@Path("{repo}")
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response repositoryHealth(@PathParam("repo") String repo,
@HeaderParam("Authorization") String auth) throws IOException {
if (!Validate.isBearer(auth)) {
log.info("api.health#protected-repository");
if (!Validate.isUnGuarded())
return new OopsResponse("api.health#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.health#repository-missing", Response.Status.BAD_REQUEST).asJSON();
}
Repository repository = this.platform.getRepository(repo);
boolean healthy = (repository != null && repository.isInitialized());
log.info("healthy.repo: {}", (healthy?repository.getDataDir():"n/a"));
return new HealthCheck(healthy ? "ok" : "api.health#repository-offline").asJSON();
}
}
