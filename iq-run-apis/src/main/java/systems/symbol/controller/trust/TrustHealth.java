package systems.symbol.controller.trust;

import com.auth0.jwt.interfaces.DecodedJWT;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * RESTful endpoint for checking the health status of the platform and RDF repositories.
 */
@Path("trust")
public class TrustHealth extends GuardedAPI {

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
@Path("health")
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response trustedHealth(@HeaderParam("Authorization") String auth) {
DecodedJWT jwt;
try {
jwt = authenticate(auth);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
return new HealthCheck("valid", jwt.getSubject()).asJSON();
}

}
