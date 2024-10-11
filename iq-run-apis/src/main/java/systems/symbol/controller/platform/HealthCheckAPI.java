package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.repository.Repository;

import systems.symbol.controller.responses.CORSResponse;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.IOException;

/**
 * RESTful endpoint for checking the health status of the platform and RDF
 * repositories.
 */
@Path("health")
public class HealthCheckAPI extends GuardedAPI {

// /**
// * CORS pre-flight
// */
// @OPTIONS
// @Produces(MediaType.APPLICATION_JSON)
// public Response preflight(@Context UriInfo info) {
// log.info("health.preflight: {}", info.getRequestUri());
// return new CORSResponse().build();
// }

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
@GET
@Produces(MediaType.APPLICATION_JSON)
public HealthCheck check() {
return new HealthCheck("ok");

}

/**
 * Checks the health status of a specific repository.
 *
 * @param _realm The name of the repository to check.
 * @return HealthCheck response indicating the repository's health status.
 */
@Path("{realm}")
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response check(@PathParam("realm") String _realm,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {
if (!Validate.isBearer(auth)) {
log.info("health.protected-repository");
return new OopsResponse("api.health.unauthorized", Response.Status.UNAUTHORIZED).build();
}
if (Validate.isNonAlphanumeric(_realm)) {
return new OopsResponse("api.health.realm_invalid", Response.Status.BAD_REQUEST).build();
}
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("api.health.realm", Response.Status.NOT_FOUND).build();
Repository repository = realm.getRepository();
boolean healthy = (repository != null && repository.isInitialized());
log.info("healthy.repo: {}", (healthy ? "yes" : "no"));
return new HealthCheck(healthy ? "ok" : "oops").build();
}
}
