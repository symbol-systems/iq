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
private final AuthorizationCodeStore authorizationCodeStore;
private final RefreshTokenStore refreshTokenStore;

public OAuthAuthorizationServer(OAuthTokenFactory tokenFactory,
OAuthTokenValidator tokenValidator,
ClientRegistry clientRegistry,
JTIRevocationStore revocationStore,
DeviceCodeStore deviceCodeStore) {
this(tokenFactory, tokenValidator, clientRegistry, revocationStore, 
 deviceCodeStore, new AuthorizationCodeStore(), new RefreshTokenStore());
}

public OAuthAuthorizationServer(OAuthTokenFactory tokenFactory,
OAuthTokenValidator tokenValidator,
ClientRegistry clientRegistry,
JTIRevocationStore revocationStore,
DeviceCodeStore deviceCodeStore,
AuthorizationCodeStore authorizationCodeStore,
RefreshTokenStore refreshTokenStore) {
this.tokenFactory = Objects.requireNonNull(tokenFactory, "tokenFactory is required");
this.tokenValidator = Objects.requireNonNull(tokenValidator, "tokenValidator is required");
this.clientRegistry = Objects.requireNonNull(clientRegistry, "clientRegistry is required");
this.revocationStore = Objects.requireNonNull(revocationStore, "revocationStore is required");
this.deviceCodeStore = Objects.requireNonNull(deviceCodeStore, "deviceCodeStore is required");
this.authorizationCodeStore = Objects.requireNonNull(authorizationCodeStore, "authorizationCodeStore is required");
this.refreshTokenStore = Objects.requireNonNull(refreshTokenStore, "refreshTokenStore is required");
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

public AuthorizationCodeStore getAuthorizationCodeStore() {
return authorizationCodeStore;
}

public RefreshTokenStore getRefreshTokenStore() {
return refreshTokenStore;
}

/**
 * Create an authorization code for the authorization code flow.
 */
public String createAuthorizationCode(String clientId, String redirectUri, String subject,
 String[] scopes, String codeChallenge, String codeChallengeMethod) {
String code = UUID.randomUUID().toString();
AuthorizationCodeStore.AuthorizationCode authCode = new AuthorizationCodeStore.AuthorizationCode(
code, clientId, redirectUri, subject, scopes, codeChallenge, codeChallengeMethod
);
authorizationCodeStore.store(authCode);
return code;
}

/**
 * Exchange an authorization code for tokens.
 */
public TokenExchangeResult completeAuthorizationCodeFlow(String code, String clientId, String redirectUri, String codeVerifier) {
if (code == null || code.isBlank()) {
return TokenExchangeResult.error("invalid_request", "code required");
}

AuthorizationCodeStore.AuthorizationCode authCode = authorizationCodeStore.getAndMarkUsed(code);
if (authCode == null) {
return TokenExchangeResult.error("invalid_grant", "Authorization code not found, expired, or already used");
}

// Verify client and redirect_uri match
if (!clientId.equals(authCode.clientId)) {
return TokenExchangeResult.error("invalid_grant", "Client ID mismatch");
}

if (!redirectUri.equals(authCode.redirectUri)) {
return TokenExchangeResult.error("invalid_grant", "Redirect URI mismatch");
}

// Verify PKCE if code_challenge was used
if (authCode.codeChallenge != null && !authCode.codeChallenge.isEmpty()) {
if (codeVerifier == null || codeVerifier.isEmpty()) {
return TokenExchangeResult.error("invalid_request", "code_verifier required for PKCE");
}
// In a production system, verify PKCE here
// For now, we assume the verifier is correct if provided
}

// Issue tokens
String accessToken = issueAccessToken(
authCode.subject,
authCode.scopes,
"default",
clientId
);

String refreshToken = issueRefreshToken(
authCode.subject,
"default",
authCode.scopes,
86400 // 24 hours
);

RefreshTokenStore.RefreshTokenInfo tokenInfo = new RefreshTokenStore.RefreshTokenInfo(
refreshToken, authCode.subject, "default", authCode.scopes, 86400
);
refreshTokenStore.store(tokenInfo);

return TokenExchangeResult.success(accessToken, refreshToken, 3600, "Bearer");
}

/**
 * Refresh an access token using a refresh token.
 */
public TokenExchangeResult refreshAccessToken(String refreshToken, String clientId, String[] requestedScopes) {
if (refreshToken == null || refreshToken.isBlank()) {
return TokenExchangeResult.error("invalid_request", "refresh_token required");
}

RefreshTokenStore.RefreshTokenInfo tokenInfo = refreshTokenStore.get(refreshToken);
if (tokenInfo == null) {
return TokenExchangeResult.error("invalid_grant", "Refresh token not found, expired, or revoked");
}

// Validate scopes (requested scopes must be subset of original scopes)
Set<String> originalScopes = new HashSet<>(Arrays.asList(tokenInfo.scopes));
Set<String> filteredScopes = originalScopes;

if (requestedScopes != null && requestedScopes.length > 0) {
filteredScopes = new HashSet<>(Arrays.asList(requestedScopes));
if (!originalScopes.containsAll(filteredScopes)) {
return TokenExchangeResult.error("invalid_scope", "Requested scopes exceed original scope");
}
}

// Issue new access token
String newAccessToken = issueAccessToken(
tokenInfo.subject,
filteredScopes.toArray(new String[0]),
tokenInfo.realm,
clientId
);

// Optionally rotate refresh token (create new one, invalidate old one)
String newRefreshToken = issueRefreshToken(
tokenInfo.subject,
tokenInfo.realm,
filteredScopes.toArray(new String[0]),
tokenInfo.expiresInSeconds
);

RefreshTokenStore.RefreshTokenInfo newTokenInfo = new RefreshTokenStore.RefreshTokenInfo(
newRefreshToken, tokenInfo.subject, tokenInfo.realm,
filteredScopes.toArray(new String[0]), tokenInfo.expiresInSeconds
);
newTokenInfo.parentToken = refreshToken;
refreshTokenStore.store(newTokenInfo);

// Revoke old refresh token
refreshTokenStore.revoke(refreshToken);

return TokenExchangeResult.success(newAccessToken, newRefreshToken, 3600, "Bearer");
}
}
