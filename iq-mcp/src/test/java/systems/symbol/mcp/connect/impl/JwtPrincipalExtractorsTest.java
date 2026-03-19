package systems.symbol.mcp.connect.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtPrincipalExtractorsTest {

    @Test
    void testFromHmacSecretValidToken() throws Exception {
        String secret = "super-secret-key-super-secret-key";
        String issuer = "https://example.com";
        String audience = "mcp-client";

        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build();

        String token = JWT.create()
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject("test-user")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(algorithm);

        AuthGuardMiddleware.JwtPrincipalExtractor extractor = JwtPrincipalExtractors.fromHmacSecret(secret, issuer, audience);
        String principal = extractor.extractPrincipal(token);

        assertEquals("test-user", principal);

        // sanity-check the token is valid to ensure we generated a good token
        DecodedJWT decoded = verifier.verify(token);
        assertEquals("test-user", decoded.getSubject());
    }

    @Test
    void testFromHmacSecretRejectsWrongIssuer() throws Exception {
        String secret = "super-secret-key-super-secret-key";
        String issuer = "https://example.com";
        String audience = "mcp-client";

        Algorithm algorithm = Algorithm.HMAC256(secret);
        String token = JWT.create()
                .withIssuer("https://evil.com")
                .withAudience(audience)
                .withSubject("test-user")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(algorithm);

        AuthGuardMiddleware.JwtPrincipalExtractor extractor = JwtPrincipalExtractors.fromHmacSecret(secret, issuer, audience);
        assertThrows(IllegalArgumentException.class, () -> extractor.extractPrincipal(token));
    }
}
