package systems.symbol.auth.oauth;

import com.auth0.jwt.JWTCreator;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.trust.generate.JWTGen;

import java.security.KeyPair;
import java.util.*;

/**
 * OAuth Token Factory for creating and signing JWT access tokens.
 * Extends JWTGen with OAuth-specific claims and configuration.
 */
public class OAuthTokenFactory {

private final JWTGen jwtGen;
private final KeyPair keyPair;
private final String issuer;
private final int tokenDurationSeconds;
private final String kid; // Key ID for JWKS

public OAuthTokenFactory(JWTGen jwtGen, KeyPair keyPair, String issuer, int tokenDurationSeconds, String kid) {
Objects.requireNonNull(jwtGen, "jwtGen is required");
Objects.requireNonNull(keyPair, "keyPair is required");
Objects.requireNonNull(issuer, "issuer is required");

this.jwtGen = jwtGen;
this.keyPair = keyPair;
this.issuer = issuer;
this.tokenDurationSeconds = tokenDurationSeconds > 0 ? tokenDurationSeconds : 3600; // 1h default
this.kid = kid;
}

/**
 * Create an access token with OAuth claims.
 * 
 * @param subject the principal identifier (e.g., user alias)
 * @param scopes the granted scopes
 * @param realm the realm identifier
 * @param audience the intended audience(s)
 * @return the signed JWT token
 */
public String createAccessToken(String subject, String[] scopes, String realm, String... audience) {
Objects.requireNonNull(subject, "subject is required");
Objects.requireNonNull(scopes, "scopes is required");
Objects.requireNonNull(realm, "realm is required");

JWTCreator.Builder builder = jwtGen.generate(issuer, subject, audience, tokenDurationSeconds)
.withClaim("realm", realm)
.withClaim("scope", String.join(" ", scopes))
.withClaim("jti", UUID.randomUUID().toString())
.withClaim("token_type", "Bearer");

return jwtGen.sign(builder, keyPair);
}

/**
 * Create a refresh token.
 * Refresh tokens are typically longer-lived and opaque.
 * 
 * @param subject the principal
 * @param realm the realm
 * @param scopes the original scopes
 * @param durationSeconds how long the refresh token is valid
 * @return the refresh token JWT
 */
public String createRefreshToken(String subject, String realm, String[] scopes, int durationSeconds) {
Objects.requireNonNull(subject, "subject is required");
Objects.requireNonNull(realm, "realm is required");

JWTCreator.Builder builder = jwtGen.generate(issuer, subject, new String[] { "refresh" }, durationSeconds)
.withClaim("realm", realm)
.withClaim("scope", String.join(" ", scopes))
.withClaim("jti", UUID.randomUUID().toString())
.withClaim("token_type", "Refresh");

return jwtGen.sign(builder, keyPair);
}

/**
 * Create a device code token for RFC 8628 device flow.
 * 
 * @param deviceCode the device code
 * @param userCode the user code displayed on device
 * @param realm the realm
 * @return the device code JWT
 */
public String createDeviceCodeToken(String deviceCode, String userCode, String realm) {
Objects.requireNonNull(deviceCode, "deviceCode is required");
Objects.requireNonNull(userCode, "userCode is required");
Objects.requireNonNull(realm, "realm is required");

int ttl = 300; // 5 minute default per RFC 8628
JWTCreator.Builder builder = jwtGen.generate(issuer, deviceCode, new String[] { realm }, ttl)
.withClaim("user_code", userCode)
.withClaim("realm", realm)
.withClaim("jti", UUID.randomUUID().toString())
.withClaim("token_type", "DeviceCode");

return jwtGen.sign(builder, keyPair);
}

/**
 * Get the Key ID for JWKS endpoint.
 */
public String getKid() {
return kid != null ? kid : "default-key";
}

/**
 * Get the issuer identifier.
 */
public String getIssuer() {
return issuer;
}

/**
 * Get the key pair.
 */
public KeyPair getKeyPair() {
return keyPair;
}
}
