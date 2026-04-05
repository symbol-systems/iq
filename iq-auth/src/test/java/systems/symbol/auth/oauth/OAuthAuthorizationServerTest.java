package systems.symbol.auth.oauth;

import com.auth0.jwt.JWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import systems.symbol.trust.generate.JWTGen;
import systems.symbol.trust.SimpleKeyStore;

import java.io.File;
import java.security.KeyPair;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class OAuthAuthorizationServerTest {

private OAuthAuthorizationServer authServer;
private KeyPair keyPair;

@BeforeEach
void setup() throws Exception {
File tmpDir = new File("tested/tmp/oauth");
tmpDir.mkdirs();
SimpleKeyStore keyStore = new SimpleKeyStore(tmpDir);
keyPair = keyStore.keys();

JWTGen jwtGen = new JWTGen();
OAuthTokenFactory tokenFactory = new OAuthTokenFactory(
jwtGen, keyPair, "https://localhost:8080", 3600, "test-key"
);

Set<String> trustedIssuers = new HashSet<>();
trustedIssuers.add("https://localhost:8080");

JTIRevocationStore revocationStore = new JTIRevocationStore();
OAuthTokenValidator tokenValidator = new OAuthTokenValidator(
keyPair, revocationStore, trustedIssuers, 30
);

ClientRegistry clientRegistry = new ClientRegistry();
ClientRegistry.OAuthClient client = new ClientRegistry.OAuthClient(
"test-client", "secret", 
Set.of("http://localhost:8080/callback"),
Set.of("authorization_code", "refresh_token", "urn:ietf:params:oauth:grant-type:device_code"),
Set.of("chat.read", "chat.write", "data:query"),
"Test Client", false
);
clientRegistry.register(client);

DeviceCodeStore deviceCodeStore = new DeviceCodeStore();

authServer = new OAuthAuthorizationServer(
tokenFactory, tokenValidator, clientRegistry, revocationStore, deviceCodeStore
);
}

@Test
void testIssueAccessToken() {
String token = authServer.issueAccessToken(
"alice", 
new String[]{"chat.read", "chat.write"},
"default",
"test-client"
);

assertNotNull(token);
assertFalse(token.isBlank());

// Verify token structure (3 parts separated by dots)
String[] parts = token.split("\\.");
assertEquals(3, parts.length);
}

@Test
void testTokenIntrospection() {
String accessToken = authServer.issueAccessToken(
"alice",
new String[]{"chat.read"},
"default",
"test-client"
);

OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(accessToken);

assertTrue(intro.active, "Token should be active");
assertEquals("alice", intro.sub);
assertEquals("default", intro.realm);
assertArrayEquals(new String[]{"chat.read"}, intro.scopes);
}

@Test
void testRevokeToken() {
String token = authServer.issueAccessToken(
"alice",
new String[]{"chat.read"},
"default",
"test-client"
);

// Extract JTI from token
String jti = JWT.decode(token).getClaim("jti").asString();
assertNotNull(jti);

// Revoke it
authServer.revokeToken(jti);

// Introspect should show inactive
OAuthTokenValidator.TokenIntrospection intro = authServer.introspect(token);
assertFalse(intro.active, "Revoked token should be inactive");
}

@Test
void testDeviceCodeFlow() {
// Initiate device code flow
DeviceCodeStore.DeviceCodeRequest request = authServer.initiateDeviceFlow(
"default",
new String[]{"chat.read"}
);

assertNotNull(request.deviceCode);
assertNotNull(request.userCode);
assertFalse(request.isApproved());

// Approve the device code
authServer.getDeviceCodeStore().approve(request.userCode, "alice");

DeviceCodeStore.DeviceCodeRequest approved = authServer.getDeviceCodeStore().get(request.deviceCode);
assertTrue(approved.isApproved());
assertEquals("alice", approved.approvedBy);

// Exchange for token
OAuthAuthorizationServer.TokenExchangeResult result = 
authServer.completeDeviceFlow(request.deviceCode, "test-client");

assertTrue(result.success);
assertNotNull(result.accessToken);
assertNotNull(result.refreshToken);
assertEquals(3600, result.expiresIn);
assertEquals("Bearer", result.tokenType);
}

@Test
void testClientRegistry() {
ClientRegistry registry = authServer.getClientRegistry();

// Validate known client
assertTrue(registry.validateClientSecret("test-client", "secret"));
assertFalse(registry.validateClientSecret("test-client", "wrong-secret"));
assertFalse(registry.validateClientSecret("unknown-client", "secret"));

// Check grant types
assertTrue(registry.isGrantTypeAllowed("test-client", "authorization_code"));
assertTrue(registry.isGrantTypeAllowed("test-client", "urn:ietf:params:oauth:grant-type:device_code"));
assertFalse(registry.isGrantTypeAllowed("test-client", "client_credentials"));

// Check scopes
Set<String> filtered = registry.filterScopes("test-client", 
Set.of("chat.read", "chat.write", "admin"));
assertEquals(2, filtered.size());
assertTrue(filtered.contains("chat.read"));
assertTrue(filtered.contains("chat.write"));
assertFalse(filtered.contains("admin"));
}
}
