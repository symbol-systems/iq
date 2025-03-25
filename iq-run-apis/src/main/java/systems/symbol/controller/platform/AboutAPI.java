package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.realm.About;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * RESTful endpoint measuring IQ realms.
 */
@Path("about")
public class AboutAPI extends GuardedAPI {

    /**
     * Checks the health status of a specific repository.
     *
     * @param _realm The name of the repository to check.
     * @return HealthCheck response indicating the repository's health status.
     */
    @Path("phi/{realm}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response phi(@PathParam("realm") String _realm,
            @HeaderParam("Authorization") String auth)
            throws IOException, SecretsException {
        if (!Validate.isBearer(auth)) {
            log.info("about.phi.protected-repository");
            return new OopsResponse("about.phi.unauthorized", Response.Status.UNAUTHORIZED).build();
        }
        if (Validate.isNonAlphanumeric(_realm)) {
            return new OopsResponse("about.phi.realm_invalid", Response.Status.BAD_REQUEST).build();
        }
        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("about.phi.realm", Response.Status.NOT_FOUND).build();

        double phi = About.computePhiNormal(realm.getModel());

        Map<String, Object> binding = new HashMap<>();
        binding.put("phi", phi);
        binding.put("size", realm.getModel().size());
        log.info("about.phi: {} == {}", _realm, binding);
        return new SimpleResponse(binding).build();
    }
}
