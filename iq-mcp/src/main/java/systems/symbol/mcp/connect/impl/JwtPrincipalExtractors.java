package systems.symbol.mcp.connect.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Objects;

/**
 * Helper factories for producing {@link AuthGuardMiddleware.JwtPrincipalExtractor}
 * implementations backed by Auth0 java-jwt.
 */
public final class JwtPrincipalExtractors {

    private JwtPrincipalExtractors() {
    }

    /**
     * Create an extractor that validates an HS256-signed token.
     *
     * @param sharedSecret     secret key used to validate the signature (HMAC)
     * @param expectedIssuer   expected issuer (iss claim), nullable to skip check
     * @param expectedAudience expected audience (aud claim), nullable to skip check
     */
    public static AuthGuardMiddleware.JwtPrincipalExtractor fromHmacSecret(
            String sharedSecret,
            String expectedIssuer,
            String expectedAudience) {
        Objects.requireNonNull(sharedSecret, "sharedSecret");

        Algorithm algorithm = Algorithm.HMAC256(sharedSecret);
        var verification = JWT.require(algorithm);
        if (expectedIssuer != null) {
            verification = verification.withIssuer(expectedIssuer);
        }
        if (expectedAudience != null) {
            verification = verification.withAudience(expectedAudience);
        }
        JWTVerifier verifier = verification.build();

        return jwt -> {
            try {
                var decoded = verifier.verify(jwt);
                String subject = decoded.getSubject();
                return (subject != null && !subject.isBlank()) ? subject : "anonymous";
            } catch (JWTVerificationException ex) {
                throw new IllegalArgumentException("JWT verification failed: " + ex.getMessage(), ex);
            }
        };
    }

    /**
     * Create an extractor that validates RSA-signed tokens using a JWKS endpoint.
     *
     * @param jwksUrl          URL of the JWKS endpoint
     * @param expectedIssuer   expected issuer (iss claim), nullable to skip check
     * @param expectedAudience expected audience (aud claim), nullable to skip check
     */
    public static AuthGuardMiddleware.JwtPrincipalExtractor fromJwksUrl(
            String jwksUrl,
            String expectedIssuer,
            String expectedAudience,
            Long cacheTtlMs) {
        Objects.requireNonNull(jwksUrl, "jwksUrl");
        try {
            JwkProvider provider = new UrlJwkProvider(new URL(jwksUrl));
            if (cacheTtlMs != null && cacheTtlMs > 0) {
                provider = new CachingJwkProvider(provider, cacheTtlMs);
            }
            return fromJwkProvider(provider, expectedIssuer, expectedAudience);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid JWKS URL: " + jwksUrl, e);
        }
    }

    /**
     * Create an extractor by performing OIDC discovery.
     *
     * <p>The discovery document MUST contain {@code issuer} and {@code jwks_uri}.
     */
    public static AuthGuardMiddleware.JwtPrincipalExtractor fromOidcDiscoveryUrl(
            String oidcUrl,
            String expectedIssuerOverride,
            String expectedAudience,
            Long cacheTtlMs) {
        Objects.requireNonNull(oidcUrl, "oidcDiscoveryUrl");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(oidcUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("OIDC discovery failed: HTTP " + response.statusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            String issuer = root.path("issuer").asText(null);
            String jwksUri = root.path("jwks_uri").asText(null);

            if (jwksUri == null || jwksUri.isBlank()) {
                throw new IllegalArgumentException("OIDC discovery document missing jwks_uri");
            }
            if (issuer == null || issuer.isBlank()) {
                throw new IllegalArgumentException("OIDC discovery document missing issuer");
            }
            String effectiveIssuer = (expectedIssuerOverride != null && !expectedIssuerOverride.isBlank())
                    ? expectedIssuerOverride
                    : issuer;

            return fromJwksUrl(jwksUri, effectiveIssuer, expectedAudience, cacheTtlMs);
        } catch (Exception e) {
            throw new IllegalArgumentException("OIDC discovery failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create an extractor using a custom JWK provider.
     */
    public static AuthGuardMiddleware.JwtPrincipalExtractor fromJwkProvider(
            JwkProvider provider,
            String expectedIssuer,
            String expectedAudience) {
        Objects.requireNonNull(provider, "provider");

        return jwt -> {
            try {
                var decoded = JWT.decode(jwt);
                String kid = decoded.getKeyId();
                if (kid == null) {
                    throw new IllegalArgumentException("JWT missing kid header");
                }
                Jwk jwk = provider.get(kid);
                Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
                var verification = JWT.require(algorithm);
                if (expectedIssuer != null) {
                    verification = verification.withIssuer(expectedIssuer);
                }
                if (expectedAudience != null) {
                    verification = verification.withAudience(expectedAudience);
                }
                JWTVerifier verifier = verification.build();
                var verified = verifier.verify(jwt);
                String subject = verified.getSubject();
                return (subject != null && !subject.isBlank()) ? subject : "anonymous";
            } catch (JwkException | JWTVerificationException ex) {
                throw new IllegalArgumentException("JWT verification failed: " + ex.getMessage(), ex);
            }
        };
    }

    /**
     * Cache wrapper for a {@link JwkProvider}.
     */
    static final class CachingJwkProvider implements JwkProvider {
        private final JwkProvider delegate;
        private final long ttlMs;
        private final java.util.concurrent.ConcurrentHashMap<String, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();

        CachingJwkProvider(JwkProvider delegate, long ttlMs) {
            this.delegate = delegate;
            this.ttlMs = ttlMs;
        }

        @Override
        public Jwk get(String keyId) throws JwkException {
            long now = System.currentTimeMillis();
            CacheEntry entry = cache.get(keyId);
            if (entry != null && now < entry.expiresAt) {
                return entry.jwk;
            }
            Jwk jwk = delegate.get(keyId);
            cache.put(keyId, new CacheEntry(jwk, now + ttlMs));
            return jwk;
        }

        public java.util.List<Jwk> getAll() throws JwkException {
            return ((UrlJwkProvider) delegate).getAll();
        }

        private record CacheEntry(Jwk jwk, long expiresAt) {}
    }
}
