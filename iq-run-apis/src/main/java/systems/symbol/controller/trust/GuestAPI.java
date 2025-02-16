package systems.symbol.controller.trust;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.controller.platform.RealmAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.platform.WebURLs;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.trust.generate.JWTGen;
import systems.symbol.util.IdentityHelper;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Path("guest")
public class GuestAPI extends RealmAPI {

    private static final ConcurrentHashMap<String, TokenBucket> tokenBuckets = new ConcurrentHashMap<>();

    @ConfigProperty(name = "iq.realm.token.duration", defaultValue = "600")
    int tokenDuration;

    @ConfigProperty(name = "iq.realm.token.limit", defaultValue = "5")
    int tokenLimitPerIP;

    @ConfigProperty(name = "iq.realm.token.reset.hours", defaultValue = "1")
    int tokenResetHours;

    /**
     * CORS pre-flight
     */
    @OPTIONS
    @Path("{path : .*}")
    public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
        log.info("guest.preflight: {} -> {} @ {}", getClass().getName(), path, info.getRequestUri());
        return new CORSResponse().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("token/{realm}")
    public Response guest(@PathParam("realm") String _realm, @Context UriInfo info, @Context HttpHeaders headers)
            throws SecretsException {
        if (Validate.isMissing(_realm))
            return new OopsResponse("guest.token.realm", Response.Status.BAD_REQUEST).build();

        String baseUrl = WebURLs.getRequestURL(info, headers);
        String clientIp = info.getRequestUri().getHost();
        log.info("guest.token: {} @ {}", baseUrl, clientIp);
        if (exceedsTokenLimit(clientIp))
            return new OopsResponse("guest.token.limit", Response.Status.TOO_MANY_REQUESTS).build();

        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("guest.token.realm", Response.Status.NOT_FOUND).build();

        String guestToken = tokenize("guest", new String[] { "guest" }, "ipv4:" + clientIp, "guest",
                new String[] { _realm },
                realm, tokenDuration);

        return new SimpleResponse("access_token", guestToken).build();
    }

    private boolean exceedsTokenLimit(String clientIp) {
        TokenBucket bucket = tokenBuckets.compute(clientIp, (ip, existingBucket) -> {
            if (existingBucket == null || existingBucket.isExpired(tokenResetHours)) {
                return new TokenBucket();
            }
            existingBucket.increment();
            return existingBucket;
        });

        return bucket.getCount() > tokenLimitPerIP;
    }

    private static String tokenize(String issuer, String[] roles, String self, String name, String[] audience,
            I_Realm realm, int duration) throws SecretsException {
        JWTGen jwtGen = new JWTGen();
        JWTCreator.Builder generator = jwtGen.generate(issuer, self, audience, duration)
                .withClaim("name", name)
                .withClaim("jti", IdentityHelper.uuid());

        if (roles.length > 0) {
            generator.withArrayClaim("roles", roles);
        }

        return jwtGen.sign(generator, realm.keys());
    }

    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        return true;
    }

    private static class TokenBucket {
        private AtomicInteger count;
        private Instant lastReset;

        public TokenBucket() {
            this.count = new AtomicInteger(1);
            this.lastReset = Instant.now();
        }

        public void increment() {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }

        public boolean isExpired(int resetHours) {
            return Instant.now().isAfter(lastReset.plusSeconds(resetHours * 3600L));
        }
    }
}
