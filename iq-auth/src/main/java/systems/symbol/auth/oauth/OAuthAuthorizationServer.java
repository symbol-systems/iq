package systems.symbol.auth.oauth;

import systems.symbol.trust.generate.JWTGen;

import java.security.KeyPair;
import java.util.*;

/**
 * Central OAuth Authorization Server facade.
 * Orchestrates token issuance, validation, revocation, and device code flow.
 */
public class OAuthAuthorizationServer {

private final OAuthTokenFactory tokenFactory;
private final OAuthTokenValidator tokenValidator;
private final ClientRegistry clientRegistry;
private final JTIRevocationStore revocationStore;
private final DeviceCodeStore deviceCodeStore;

public OAuthAuthorizationServer(OAuthTokenFactory tokenFactory,
OAuthTokenValidator tokenValidator,
ClientRegistry clientRegistry,
JTIRevocationStore revocationStore,
DeviceCodeStore deviceCodeStore) {
this.tokenFactory = Objects.requireNonNull(tokenFactory, "tokenFactory is required");
this.tokenValidator = Objects.requireNonNull(tokenValidator, "tokenValidator is required");
this.clientRegistry = Objects.requireNonNull(clientRegistry, "clientRegistry is required");
this.revocationStore = Objects.requireNonNull(revocationStore, "revocationStore is required");
this.deviceCodeStore = Objects.requireNonNull(deviceCodeStore, "deviceCodeStore is required");
}

/**
 * Issue an access token.
 */
public String issueAccessToken(String subject, String[] scopes, String realm, String... audience) {
return tokenFactory.createAccessToken(subject, scopes, realm, audience);
}

/**
 * Issue a refresh token.
 */
public String issueRefreshToken(String subject, String realm, String[] scopes, int durationSeconds) {
return tokenFactory.createRefreshToken(subject, realm, scopes, durationSeconds);
}

/**
 * Introspect (validate) a token.
 */
public OAuthTokenValidator.TokenIntrospection introspect(String token) {
return tokenValidator.introspect(token);
}

/**
 * Revoke a token by its JTI.
 */
public void revokeToken(String jti) {
Objects.requireNonNull(jti, "jti is required");
revocationStore.revoke(jti);
}

/**
 * Initiate RFC 8628 device authorization flow.
 */
public DeviceCodeStore.DeviceCodeRequest initiateDeviceFlow(String realm, String[] requestedScopes) {
Objects.requireNonNull(realm, "realm is required");

String deviceCode = UUID.randomUUID().toString();
String userCode = generateUserCode();
String verificationUri = "http://localhost:8080/oauth/device/activate";
int expiresIn = 300; // 5 minutes per RFC 8628
int interval = 5; // 5 second polling interval

DeviceCodeStore.DeviceCodeRequest request = new DeviceCodeStore.DeviceCodeRequest(
deviceCode, userCode, verificationUri, expiresIn, interval, realm, requestedScopes
);

deviceCodeStore.store(request);
return request;
}

/**
 * Complete device flow - issue token after user approval.
 */
public TokenExchangeResult completeDeviceFlow(String deviceCode, String clientId) {
Objects.requireNonNull(deviceCode, "deviceCode is required");
Objects.requireNonNull(clientId, "clientId is required");

DeviceCodeStore.DeviceCodeRequest request = deviceCodeStore.get(deviceCode);
if (request == null) {
return TokenExchangeResult.error("invalid_grant", "Device code not found or expired");
}

if (!request.isApproved()) {
return TokenExchangeResult.error("authorization_pending", "User has not yet approved");
}

// Filter scopes based on client permissions
Set<String> filteredScopes = clientRegistry.filterScopes(clientId, 
new HashSet<>(Arrays.asList(request.requestedScopes)));

if (filteredScopes.isEmpty()) {
return TokenExchangeResult.error("access_denied", "Client not authorized for requested scopes");
}

// Issue tokens
String accessToken = issueAccessToken(
request.approvedBy,
filteredScopes.toArray(new String[0]),
request.realm,
clientId
);

String refreshToken = issueRefreshToken(
request.approvedBy,
request.realm,
filteredScopes.toArray(new String[0]),
86400 // 24 hours
);

// Remove device code to prevent replay
deviceCodeStore.remove(deviceCode);

return TokenExchangeResult.success(accessToken, refreshToken, 3600, "Bearer");
}

/**
 * Generate a user code for device flow (8 alphanumeric characters).
 */
private String generateUserCode() {
String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
StringBuilder code = new StringBuilder();
Random random = new Random();
for (int i = 0; i < 8; i++) {
code.append(chars.charAt(random.nextInt(chars.length())));
}
return code.toString();
}

/**
 * Token exchange result.
 */
public static class TokenExchangeResult {
public final boolean success;
public final String accessToken;
public final String refreshToken;
public final int expiresIn;
public final String tokenType;
public final String errorCode;
public final String errorDescription;

private TokenExchangeResult(boolean success, String accessToken, String refreshToken,
   int expiresIn, String tokenType, String errorCode, String errorDescription) {
this.success = success;
this.accessToken = accessToken;
this.refreshToken = refreshToken;
this.expiresIn = expiresIn;
this.tokenType = tokenType;
this.errorCode = errorCode;
this.errorDescription = errorDescription;
}

public static TokenExchangeResult success(String accessToken, String refreshToken, 
  int expiresIn, String tokenType) {
return new TokenExchangeResult(true, accessToken, refreshToken, expiresIn, tokenType, null, null);
}

public static TokenExchangeResult error(String errorCode, String errorDescription) {
return new TokenExchangeResult(false, null, null, 0, null, errorCode, errorDescription);
}
}

// Getters for components

public OAuthTokenFactory getTokenFactory() {
return tokenFactory;
}

public OAuthTokenValidator getTokenValidator() {
return tokenValidator;
}

public ClientRegistry getClientRegistry() {
return clientRegistry;
}

public JTIRevocationStore getRevocationStore() {
return revocationStore;
}

public DeviceCodeStore getDeviceCodeStore() {
return deviceCodeStore;
}
}
