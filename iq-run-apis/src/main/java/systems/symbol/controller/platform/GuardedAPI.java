package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.platform.Platform;
import systems.symbol.string.Validate;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.generate.JWTGen;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * RESTful endpoint for checking the health status of the platform and RDF repositories.
 */
public class GuardedAPI {
protected final static Logger log = LoggerFactory.getLogger(GuardedAPI.class);

@Inject
protected Platform platform;

/**
 * CORS pre-flight
 */
@OPTIONS
@Path("{path : .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight(@PathParam("path") String path) {
log.info("preflight: {}", path);
return new DataResponse(null).asJSON();
}

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
public DecodedJWT authenticate(String bearer) throws OopsException {
return decode(bearer, platform);
}

public DecodedJWT authenticate(String auth, String claim, String[] needs) throws OopsException {
if (!Validate.isBearer(auth))
throw new OopsException("api.import#unauthorized", Response.Status.UNAUTHORIZED);

DecodedJWT jwt = authenticate(auth);
if (jwt == null)
throw new OopsException("api.ux.agent#token-invalid", Response.Status.FORBIDDEN);

Claim claims = jwt.getClaim(claim);
if (claims == null || claims.isMissing())
throw new OopsException("api.ux.agent#claims-missing", Response.Status.FORBIDDEN);

String[] roles = claims.asArray(String.class);
if (roles == null || roles.length==0)
throw new OopsException("api.ux.agent#roles-missing", Response.Status.FORBIDDEN);

for (String n : needs) {
if (!Arrays.asList(roles).contains(n))
throw new OopsException("api.ux.agent#claims-invalid", Response.Status.FORBIDDEN);
}
return jwt;
}
/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
public static DecodedJWT decode(String bearer, I_Keys keys) throws OopsException {
log.info("jwt.header: {}", bearer);
if (!Validate.isBearer(bearer)) {
throw new OopsException("api.trust.required", Response.Status.UNAUTHORIZED);
}
JWTGen jwtGen = new JWTGen();
String token = bearer.substring("BEARER ".length());
try {
return jwtGen.verify(keys.keys(), token);
} catch (Exception e) {
//log.error("jwt.reject: {} -> {}", token, e.getMessage());
throw new OopsException("api.trust.reject", Response.Status.FORBIDDEN);
}
}

}
