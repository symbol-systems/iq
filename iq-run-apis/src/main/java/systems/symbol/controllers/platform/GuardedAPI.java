package systems.symbol.controllers.platform;

import systems.symbol.platform.Platform;
import systems.symbol.string.Validate;
import systems.symbol.trust.generate.JWTGen;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful endpoint for checking the health status of the platform and RDF repositories.
 */
public class GuardedAPI {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    protected Platform platform;

    /**
     * Checks the overall health status of the platform.
     *
     * @return HealthCheck response indicating the platform's health status.
     */
    public com.auth0.jwt.interfaces.DecodedJWT authenticate(String bearer) {
        log.info("authenticate: {}", bearer);
        if (!Validate.isBearer(bearer)) {
            return null;
        }
        String token = bearer.substring("BEARER ".length());
        JWTGen jwtGen = new JWTGen();
        try {
            com.auth0.jwt.interfaces.DecodedJWT verified = jwtGen.verify(platform.loadKeyPair(), token);
            return verified;
        } catch (Exception e) {
            log.error("token.broken: {}", token, e);
            return null;
        }
    }

}
