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
// import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

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
        if (jwt.getClaim(claim).isMissing())
            throw new OopsException("ux.realm.claim." + claim, Response.Status.UNAUTHORIZED);
        if (!jwt.getClaim(claim).asList(String.class).contains(clause))
            throw new OopsException("ux.realm.clause." + claim, Response.Status.UNAUTHORIZED);
        return true;
    }

    public DecodedJWT authenticate(String auth, String claim, String[] needs, I_Keys keys)
            throws OopsException, SecretsException {
        if (!Validate.isBearer(auth))
            throw new OopsException("ux.realm.unauthorized", Response.Status.UNAUTHORIZED);

        DecodedJWT jwt = authenticate(auth, keys);
        if (jwt == null)
            throw new OopsException("ux.realm.token-invalid", Response.Status.FORBIDDEN);

        Claim claims = jwt.getClaim(claim);
        if (claims == null || claims.isMissing())
            throw new OopsException("ux.realm.claims-missing", Response.Status.FORBIDDEN);

        String[] roles = claims.asArray(String.class);
        if (roles == null || roles.length == 0)
            throw new OopsException("ux.realm.roles-missing", Response.Status.FORBIDDEN);

        for (String n : needs) {
            if (!Arrays.asList(roles).contains(n))
                throw new OopsException("ux.realm.claims-invalid", Response.Status.FORBIDDEN);
        }
        return jwt;
    }

    /**
     * Checks the overall health status of the platform.
     *
     * @return HealthCheck response indicating the platform's health status.
     */
    public static DecodedJWT decode(String bearer, I_Keys keys) throws OopsException, SecretsException {
        if (bearer == null || bearer.isEmpty())
            throw new OopsException("ux.realm.bearer.missing", Response.Status.UNAUTHORIZED);
        boolean isValid = Validate.isBearer(bearer);
        if (!isValid) {
            throw new OopsException("ux.realm.bearer.trust", Response.Status.UNAUTHORIZED);
        }
        JWTGen jwtGen = new JWTGen();
        try {
            String token = bearer.substring("BEARER ".length());
            return jwtGen.verify(keys.keys(), token);
        } catch (Exception e) {
            // log.warn("aou,realm.trust: {}", e.getMessage());
            throw new OopsException("ux.realm.trust.reject", Response.Status.FORBIDDEN);
        }
    }

    public static String getFullRequestUri(HttpServletRequest request) {
        String baseUrl = getBaseURL(request);

        // Append the request URI
        String fullUri = baseUrl + request.getRequestURI();

        // Add query string if present
        if (request.getQueryString() != null) {
            fullUri += "?" + request.getQueryString();
        }

        return fullUri;
    }

    public static String getBaseURL(HttpServletRequest request) {
        // Determine the scheme (e.g., HTTP/HTTPS)
        String scheme = request.getHeader("X-Forwarded-Proto") != null
                ? request.getHeader("X-Forwarded-Proto")
                : request.getScheme();

        // Determine the host
        String host = request.getHeader("X-Forwarded-Host") != null
                ? request.getHeader("X-Forwarded-Host")
                : request.getServerName();

        // Determine the port
        String port;
        if (request.getHeader("X-Forwarded-Port") != null) {
            port = request.getHeader("X-Forwarded-Port");
        } else {
            int serverPort = request.getServerPort();
            port = (serverPort == 80 && "http".equalsIgnoreCase(scheme)) ||
                    (serverPort == 443 && "https".equalsIgnoreCase(scheme))
                            ? ""
                            : ":" + serverPort;
        }

        return scheme + "://" + host + port;
    }
}
