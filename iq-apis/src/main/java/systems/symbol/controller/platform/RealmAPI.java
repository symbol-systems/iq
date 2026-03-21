package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.controller.responses.CORSResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.generate.JWTGen;

import java.util.Arrays;

/**
 * Abstract endpoint for realm-based APIs
 */
public abstract class RealmAPI {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject
protected RealmPlatform platform;

/**
 * CORS pre-flight
 */
@OPTIONS
@Path("{path : .*}")
public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
log.debug("realm.preflight: {} -> {} @ {}", getClass().getName(), path, info.getRequestUri());
return new CORSResponse().build();
}

@OPTIONS
public Response preflight(@Context UriInfo info) {
log.debug("realm.preflight: {} -> {}", getClass().getName(), info.getRequestUri());
return new CORSResponse().build();
}

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
public DecodedJWT authenticate(String bearer, I_Keys keys) throws OopsException, SecretsException {
return decode(bearer, keys);
}

public abstract boolean entitled(DecodedJWT jwt, IRI agent);

public boolean entitled(DecodedJWT jwt, String claim, String clause) throws OopsException {
validateJWTClaim(jwt, claim);
if (!jwt.getClaim(claim).asList(String.class).contains(clause)) {
throw new OopsException("ux.realm.clause." + claim, Response.Status.UNAUTHORIZED);
}
return true;
}

public DecodedJWT authenticate(String auth, String claim, String[] needs, I_Keys keys)
throws OopsException, SecretsException {
// Validate bearer format
if (!Validate.isBearer(auth)) {
throw new OopsException("ux.realm.unauthorized", Response.Status.UNAUTHORIZED);
}

// Decode and verify JWT
DecodedJWT jwt = authenticate(auth, keys);

// Validate required roles/claims exist and are satisfied
validateJWTRoles(jwt, needs);

return jwt;
}

/**
 * Checks the overall health status of the platform.
 *
 * @return HealthCheck response indicating the platform's health status.
 */
public DecodedJWT decode(String bearer, I_Keys keys) throws OopsException, SecretsException {
// Validate bearer token is present
if (bearer == null || bearer.isEmpty()) {
throw new OopsException("ux.realm.token.missing", Response.Status.UNAUTHORIZED);
}

// Validate bearer has correct format
if (!Validate.isBearer(bearer)) {
throw new OopsException("ux.realm.bearer.missing", Response.Status.UNAUTHORIZED);
}

// Decode and verify JWT
JWTGen jwtGen = new JWTGen();
try {
String token = bearer.substring("BEARER ".length());
return jwtGen.verify(keys.keys(), token);
} catch (Exception e) {
log.warn("ux.realm.token.verify: {} -> {}", bearer.substring(0, Math.min(20, bearer.length())), e.getMessage(), e);
throw new OopsException("ux.realm.trust.reject", Response.Status.FORBIDDEN);
}
}

/**
 * Validates JWT and required claim exists.
 * @param jwt The decoded JWT
 * @param claim The claim name to check
 * @throws OopsException if JWT or claim is null/invalid
 */
protected void validateJWTClaim(DecodedJWT jwt, String claim) throws OopsException {
if (jwt == null) {
throw new OopsException("ux.realm.token-invalid", Response.Status.FORBIDDEN);
}

Claim claims = jwt.getClaim(claim);
if (claims == null || claims.isMissing()) {
throw new OopsException("ux.realm.claims-missing", Response.Status.FORBIDDEN);
}
}

/**
 * Validates JWT has required role(s).
 * @param jwt The decoded JWT
 * @param requiredRoles The role(s) that must be present
 * @return true if JWT contains at least one required role
 * @throws OopsException if validation fails
 */
protected boolean validateJWTRoles(DecodedJWT jwt, String[] requiredRoles) throws OopsException {
if (jwt == null) {
throw new OopsException("ux.realm.token-invalid", Response.Status.FORBIDDEN);
}

if (requiredRoles == null || requiredRoles.length == 0) {
return true;
}

Claim rolesClaim = jwt.getClaim("roles");
if (rolesClaim == null || rolesClaim.isMissing()) {
throw new OopsException("ux.realm.roles-missing", Response.Status.FORBIDDEN);
}

String[] roles = rolesClaim.asArray(String.class);
if (roles == null || roles.length == 0) {
throw new OopsException("ux.realm.roles-missing", Response.Status.FORBIDDEN);
}

for (String required : requiredRoles) {
if (Arrays.asList(roles).contains(required)) {
return true;
}
}

throw new OopsException("ux.realm.claims-invalid", Response.Status.FORBIDDEN);
}
}
