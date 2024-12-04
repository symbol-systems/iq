package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.repository.Repository;

import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.platform.WebURLs;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import java.io.IOException;

/**
 * RESTful endpoint for checking the health of the platform and realms.
 */
@Path("health")
public class HealthCheckAPI extends GuardedAPI {

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response check(@Context UriInfo info, @Context HttpHeaders headers) {
log.info("health.ok");
return new HealthCheck(true, WebURLs.getBaseURL(info, headers)).build();
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
@HeaderParam("Authorization") String auth, @Context UriInfo info, @Context HttpHeaders headers)
throws IOException, SecretsException {
if (!Validate.isBearer(auth)) {
log.info("health.protected-repository");
return new OopsResponse("ux.health.unauthorized", Response.Status.UNAUTHORIZED).build();
}
if (Validate.isNonAlphanumeric(_realm)) {
return new OopsResponse("ux.health.realm_invalid", Response.Status.BAD_REQUEST).build();
}
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.health.realm", Response.Status.NOT_FOUND).build();
Repository repository = realm.getRepository();
boolean healthy = (repository != null && repository.isInitialized());
log.info("healthy.realm: {} == {}", _realm, healthy);
return new HealthCheck(healthy, WebURLs.getBaseURL(info, headers)).build();
}
}
