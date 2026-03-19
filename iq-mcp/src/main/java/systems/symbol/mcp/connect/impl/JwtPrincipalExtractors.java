package systems.symbol.mcp.connect.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

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
 * @param sharedSecret secret key used to validate the signature (HMAC)
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
}
