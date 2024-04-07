package systems.symbol.controllers.trust;

import systems.symbol.controllers.platform.GuardedAPI;
import systems.symbol.responses.HealthCheck;
import systems.symbol.responses.OopsResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

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
    public Response trustedHealth(@HeaderParam("Authorization") String bearer) {
        com.auth0.jwt.interfaces.DecodedJWT jwt = authenticate(bearer);
        if (jwt==null) {
            return new OopsResponse("api.trust.issuer#token-invalid", Response.Status.FORBIDDEN).asJSON();
        }
        return new HealthCheck("valid", jwt.getSubject()).asJSON();
    }

}
