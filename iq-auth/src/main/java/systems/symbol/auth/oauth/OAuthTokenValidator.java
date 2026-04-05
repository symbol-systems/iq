package systems.symbol.auth.oauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;

import java.security.KeyPair;
import java.time.Instant;
import java.util.*;

/**
 * OAuth Token Validator for verifying and introspecting tokens.
 * Implements RFC 7662 token introspection semantics.
 */
public class OAuthTokenValidator {

private final JWTVerifier verifier;
private final JTIRevocationStore revocationStore;
private final Set<String> trustedIssuers;
private final int clockSkewSeconds;

public OAuthTokenValidator(KeyPair keyPair, JTIRevocationStore revocationStore, 
   Set<String> trustedIssuers, int clockSkewSeconds) {
Objects.requireNonNull(keyPair, "keyPair is required");
Objects.requireNonNull(revocationStore, "revocationStore is required");
Objects.requireNonNull(trustedIssuers, "trustedIssuers is required");

// Create JWT verifier from public key only
java.security.interfaces.RSAPublicKey publicKey = 
(java.security.interfaces.RSAPublicKey) keyPair.getPublic();

this.verifier = JWT.require(
com.auth0.jwt.algorithms.Algorithm.RSA256(publicKey, null)
).build();

this.revocationStore = revocationStore;
this.trustedIssuers = Collections.unmodifiableSet(
new HashSet<>(trustedIssuers)
);
this.clockSkewSeconds = Math.max(0, clockSkewSeconds);
}

/**
 * Validate a token and return introspection result.
 * 
 * @param token the JWT token string
 * @return introspection result
 */
public TokenIntrospection introspect(String token) {
try {
// Verify signature
DecodedJWT decoded = verifier.verify(token);

// Check issuer
String issuer = decoded.getIssuer();
if (issuer == null || !trustedIssuers.contains(issuer)) {
return TokenIntrospection.invalid(
"issuer not trusted: " + issuer
);
}

// Check if revoked
String jti = decoded.getClaim("jti").asString();
if (jti != null && revocationStore.isRevoked(jti)) {
return TokenIntrospection.invalid("token has been revoked");
}

// Check expiration with clock skew
Instant expiresAt = decoded.getExpiresAtAsInstant();
if (expiresAt != null) {
long skewedNow = System.currentTimeMillis() + (clockSkewSeconds * 1000L);
if (expiresAt.toEpochMilli() < skewedNow) {
return TokenIntrospection.invalid("token expired");
}
}

// Extract scopes
String scopeStr = decoded.getClaim("scope").asString();
String[] scopes = scopeStr != null ? scopeStr.split(" ") : new String[0];

// Extract realm
String realm = decoded.getClaim("realm").asString();

// Valid token
return TokenIntrospection.active(
decoded.getSubject(),
issuer,
realm,
scopes,
expiresAt
);
} catch (Exception e) {
return TokenIntrospection.invalid("token validation failed: " + e.getMessage());
}
}

/**
 * Token introspection result following RFC 7662.
 */
public static class TokenIntrospection {
public final boolean active;
public final String sub;
public final String iss;
public final String realm;
public final String[] scopes;
public final Instant exp;
public final String error;

private TokenIntrospection(boolean active, String sub, String iss, 
   String realm, String[] scopes, Instant exp, String error) {
this.active = active;
this.sub = sub;
this.iss = iss;
this.realm = realm;
this.scopes = scopes;
this.exp = exp;
this.error = error;
}

public static TokenIntrospection active(String sub, String iss, String realm, 
String[] scopes, Instant exp) {
return new TokenIntrospection(true, sub, iss, realm, scopes, exp, null);
}

public static TokenIntrospection invalid(String error) {
return new TokenIntrospection(false, null, null, null, null, null, error);
}
}
}
