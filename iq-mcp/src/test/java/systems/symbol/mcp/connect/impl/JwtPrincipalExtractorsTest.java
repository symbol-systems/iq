package systems.symbol.mcp.connect.impl;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class JwtPrincipalExtractorsTest {

    private HttpServer server;

    @BeforeEach
    void setupServer() {
        server = null;
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

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

    @Test
    void testFromJwksUrlValidToken() throws Exception {
        // Create an RSA key pair and expose the public key via an in-process JWKS server.
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey pub = (RSAPublicKey) kp.getPublic();

        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getPublicExponent().toByteArray());
        String kid = "test-key";

        String jwks = "{\"keys\":[{" +
                "\"kty\":\"RSA\",\"kid\":\"" + kid + "\",\"use\":\"sig\",\"alg\":\"RS256\"," +
                "\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}";

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = jwks.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();

        String jwksUrl = "http://localhost:" + server.getAddress().getPort() + "/jwks";
        String issuer = "https://example.com";
        String audience = "mcp-client";

        Algorithm algorithm = Algorithm.RSA256(pub, (java.security.interfaces.RSAPrivateKey) kp.getPrivate());
        String token = JWT.create()
                .withKeyId(kid)
                .withIssuer(issuer)
                .withAudience(audience)
                .withSubject("test-user")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(60)))
                .sign(algorithm);

        AuthGuardMiddleware.JwtPrincipalExtractor extractor = JwtPrincipalExtractors.fromJwksUrl(jwksUrl, issuer, audience);
        String principal = extractor.extractPrincipal(token);
        assertEquals("test-user", principal);
    }
}
