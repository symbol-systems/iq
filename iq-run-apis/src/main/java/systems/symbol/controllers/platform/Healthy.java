package systems.symbol.controllers.platform;

import systems.symbol.platform.Platform;
import systems.symbol.responses.HealthCheck;
import systems.symbol.string.Validate;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.rdf4j.repository.Repository;

/**
 * RESTful endpoint for checking the health status of the platform and RDF repositories.
 */
@Path("health")
public class Healthy {

@Inject
Platform platform;

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
public HealthCheck repositoryHealth(@PathParam("repo") String repo) {
if (Validate.isNonAlphanumeric(repo)) {
return new HealthCheck("api.health.repository#misnamed");
}
Repository repository = this.platform.getRepository(repo);
boolean healthy = (repository != null && repository.isInitialized());
System.out.println("healthy.repo: "+(healthy?repository.getDataDir():"n/a"));
return new HealthCheck(healthy ? "api.ok" : "api.health.repository.offline");
}
}
