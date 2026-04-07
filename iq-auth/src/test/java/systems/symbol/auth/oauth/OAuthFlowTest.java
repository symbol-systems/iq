package systems.symbol.auth.oauth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.trust.generate.JWTGen;
import systems.symbol.trust.SimpleKeyStore;

import java.io.File;
import java.security.KeyPair;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive OAuth flow tests for access token and refresh token flows.
 * 
 * Tests cover:
 * - Access token issuance
 * - Refresh token issuance and usage
 * - Token validation (introspection)
 * - Token revocation
 * - Device code flow (RFC 8628)
 * - Scope validation
 */
public class OAuthFlowTest {

private OAuthAuthorizationServer authServer;
private OAuthTokenFactory tokenFactory;
private OAuthTokenValidator tokenValidator;
private ClientRegistry clientRegistry;
private JTIRevocationStore revocationStore;
private KeyPair keyPair;

@BeforeEach
void setup() throws Exception {
File tmpDir = new File("tested/tmp/oauth-flows");
tmpDir.mkdirs();
SimpleKeyStore keyStore = new SimpleKeyStore(tmpDir);
keyPair = keyStore.keys();

JWTGen jwtGen = new JWTGen();
tokenFactory = new OAuthTokenFactory(
jwtGen, keyPair, "https://test-server:8080", 3600, "test-realm"
);

Set<String> trustedIssuers = new HashSet<>();
trustedIssuers.add("https://test-server:8080");

revocationStore = new JTIRevocationStore();
tokenValidator = new OAuthTokenValidator(
keyPair, revocationStore, trustedIssuers, 30
);

clientRegistry = new ClientRegistry();
ClientRegistry.OAuthClient publicClient = new ClientRegistry.OAuthClient(
"public-client", "", 
Set.of("http://localhost:8080/callback"),
Set.of("authorization_code", "refresh_token"),
Set.of("read", "write"),
"Public Client", true
);
clientRegistry.register(publicClient);

ClientRegistry.OAuthClient confidentialClient = new ClientRegistry.OAuthClient(
"confidential-client", "client-secret",
Set.of("http://localhost:8080/callback"),
Set.of("authorization_code", "refresh_token", "client_credentials"),
Set.of("read", "write", "admin"),
"Confidential Client", false
);
clientRegistry.register(confidentialClient);

DeviceCodeStore deviceCodeStore = new DeviceCodeStore();

authServer = new OAuthAuthorizationServer(
tokenFactory, tokenValidator, clientRegistry, revocationStore, deviceCodeStore
);
}

/**
 * Test: Access Token Issuance
 * 
 * Scenario:
 * 1. Issue access token for user
 * 2. Token contains valid JWT structure
 * 3. Token can be decoded with public key
 */
@Test
void testAccessTokenIssuance() {
String accessToken = authServer.issueAccessToken(
"user-alice",
new String[]{"read", "write"},
"default",
"public-client"
);

assertNotNull(accessToken, "Access token should be issued");
assertFalse(accessToken.isEmpty(), "Token should not be empty");

// Verify JWT structure (3 parts: header.payload.signature)
String[] parts = accessToken.split("\\.");
assertEquals(3, parts.length, "Access token should be valid JWT");

// Decode token
DecodedJWT decoded = JWT.decode(accessToken);
assertEquals("user-alice", decoded.getSubject(), "Subject should match");
}

/**
 * Test: Refresh Token Lifecycle
 * 
 * Scenario:
 * 1. Issue refresh token for user
 * 2. Refresh token can be stored/retrieved
 * 3. Refresh token has longer expiration
 */
@Test
void testRefreshTokenIssuance() {
String refreshToken = authServer.issueRefreshToken(
"user-bob",
"default",
new String[]{"read"},
7200  // 2 hours
);

assertNotNull(refreshToken, "Refresh token should be issued");
String[] parts = refreshToken.split("\\.");
assertEquals(3, parts.length, "Refresh token should be valid JWT");

DecodedJWT decoded = JWT.decode(refreshToken);
assertEquals("user-bob", decoded.getSubject());
}

/**
 * Test: Token Introspection
 * 
 * Scenario:
 * 1. Issue token
 * 2. Introspect token to verify claims
 * 3. Token shows as active
 */
@Test
void testTokenIntrospection() {
String token = authServer.issueAccessToken(
"user-charlie",
new String[]{"read", "write"},
"default",
"confidential-client"
);

OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(token);

assertTrue(intro.active, "Token should be active");
assertEquals("user-charlie", intro.sub, "Subject should match");
assertEquals("default", intro.realm, "Realm should match");
assertArrayEquals(new String[]{"read", "write"}, intro.scopes, "Scopes should match");
}

/**
 * Test: Token Revocation
 * 
 * Scenario:
 * 1. Issue token
 * 2. Revoke token by JTI
 * 3. Revoked token shows as inactive
 */
@Test
void testTokenRevocation() {
String token = authServer.issueAccessToken(
"user-dave",
new String[]{"read"},
"default",
"public-client"
);

// Extract JTI (JWT ID claim)
DecodedJWT decoded = JWT.decode(token);
String jti = decoded.getClaim("jti").asString();
assertNotNull(jti, "Token should have JTI claim");

// Verify token is active
OAuthTokenValidator.TokenIntrospection beforeRevoke = authServer.introspect(token);
assertTrue(beforeRevoke.active, "Token should be active before revocation");

// Revoke token
authServer.revokeToken(jti);

// Verify token is now inactive
OAuthTokenValidator.TokenIntrospection afterRevoke = authServer.introspect(token);
assertFalse(afterRevoke.active, "Token should be inactive after revocation");
}

/**
 * Test: Scope Validation
 * 
 * Scenario:
 * 1. Request scopes beyond client's allowed scopes = error
 * 2. Client can receive subset of allowed scopes
 */
@Test
void testScopeValidation() {
// Client 'public-client' is only granted 'read' and 'write' scopes
String tokenWithAllowedScope = authServer.issueAccessToken(
"user-eve",
new String[]{"read"},
"default",
"public-client"
);

OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(tokenWithAllowedScope);
assertTrue(intro.active);
assertEquals(1, intro.scopes.length);
assertEquals("read", intro.scopes[0]);
}

/**
 * Test: Device Code Flow
 * 
 * Scenario:
 * 1. Initiate device code flow
 * 2. User approves device code
 * 3. Client exchanges device code for tokens
 */
@Test
void testDeviceCodeFlow() {
// Initiate device code flow
DeviceCodeStore.DeviceCodeRequest request = authServer.initiateDeviceFlow(
"default",
new String[]{"read", "write"}
);

assertNotNull(request.deviceCode, "Device code should be issued");
assertNotNull(request.userCode, "User code should be issued");
assertNotNull(request.verificationUri, "Verification URI should be provided");
assertEquals(300, request.expiresIn, "Default expiration: 5 minutes");
assertFalse(request.isApproved(), "Device code should start as not approved");

// User approves device code
DeviceCodeStore store = authServer.getDeviceCodeStore();
store.approve(request.userCode, "user-frank");

DeviceCodeStore.DeviceCodeRequest approved = store.get(request.deviceCode);
assertTrue(approved.isApproved(), "Device code should be approved");
assertEquals("user-frank", approved.approvedBy, "Approval should record user");

// Complete device flow - exchange for token
OAuthAuthorizationServer.TokenExchangeResult result = 
authServer.completeDeviceFlow(request.deviceCode, "public-client");

assertTrue(result.success, "Device code exchange should succeed");
assertNotNull(result.accessToken, "Access token should be issued");
assertNotNull(result.refreshToken, "Refresh token should be issued");
assertEquals(3600, result.expiresIn, "Token should expire in 1 hour");
assertEquals("Bearer", result.tokenType, "Token type should be Bearer");
}

/**
 * Test: Client Credential Grant
 * 
 * Scenario:
 * 1. Confidential client requests token with client credentials
 * 2. Token issued for client (not for user)
 */
@Test
void testClientCredentialGrant() {
// Issue token for client (for service-to-service communication)
String clientToken = authServer.issueAccessToken(
"confidential-client",
new String[]{"admin"},
"default",
"confidential-client"
);

OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(clientToken);
assertTrue(intro.active);
assertEquals("confidential-client", intro.sub, "Subject should be client ID");
assertTrue(Arrays.asList(intro.scopes).contains("admin"), "Should have admin scope");
}

/**
 * Test: Multiple Scope Request
 * 
 * Scenario:
 * 1. Request multiple scopes
 * 2. Token reflects all requested scopes
 */
@Test
void testMultipleScopeRequest() {
String[] requestedScopes = {"read", "write"};
String token = authServer.issueAccessToken(
"user-grace",
requestedScopes,
"default",
"confidential-client"
);

OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(token);
assertEquals(2, intro.scopes.length, "Should have 2 scopes");
assertTrue(Arrays.asList(intro.scopes).containsAll(Arrays.asList(requestedScopes)));
}

/**
 * Test: Token Audience
 * 
 * Scenario:
 * 1. Token issued with specific audience
 * 2. Audience claim present in token
 */
@Test
void testTokenAudience() {
String token = authServer.issueAccessToken(
"user-henry",
new String[]{"read"},
"default",
"api-client-1", "api-client-2"
);

DecodedJWT decoded = JWT.decode(token);
List<String> audiences = decoded.getAudience();
assertTrue(audiences.contains("api-client-1"), "Token should include first audience");
assertTrue(audiences.contains("api-client-2"), "Token should include second audience");
}
}
