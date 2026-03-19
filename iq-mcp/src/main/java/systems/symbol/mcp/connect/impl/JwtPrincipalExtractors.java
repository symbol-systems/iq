package systems.symbol.mcp.connect.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
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
            String expectedAudience) {
        Objects.requireNonNull(jwksUrl, "jwksUrl");
        try {
            return fromJwkProvider(new UrlJwkProvider(new URL(jwksUrl)), expectedIssuer, expectedAudience);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid JWKS URL: " + jwksUrl, e);
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
}
