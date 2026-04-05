package systems.symbol.auth.oauth.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.auth.oauth.*;
import systems.symbol.trust.generate.JWTGen;
import systems.symbol.trust.SimpleKeyStore;

import java.io.File;
import java.security.KeyPair;
import java.util.*;

/**
 * CDI Producer for OAuth Authorization Server components.
 * Automatically wires and configures all OAuth services for Quarkus.
 */
@ApplicationScoped
public class OAuthProducer {

private static final Logger log = LoggerFactory.getLogger(OAuthProducer.class);

@ConfigProperty(name = "iq.oauth.issuer", defaultValue = "http://localhost:8080")
String issuer;

@ConfigProperty(name = "iq.oauth.token.duration", defaultValue = "3600")
int tokenDurationSeconds;

@ConfigProperty(name = "iq.oauth.device.ttl", defaultValue = "300")
int deviceCodeTtl;

@ConfigProperty(name = "iq.oauth.device.interval", defaultValue = "5")
int deviceCodeInterval;

@ConfigProperty(name = "iq.oauth.revocation.maxEntries", defaultValue = "10000")
int revocationMaxEntries;

@ConfigProperty(name = "iq.oauth.revocation.ttl", defaultValue = "3600")
int revocationTtl;

@ConfigProperty(name = "iq.oauth.clock.skew", defaultValue = "30")
int clockSkewSeconds;

@ConfigProperty(name = "iq.oauth.keystore.path", defaultValue = ".iq/oauth/keys")
String keystorePath;

private KeyPair keyPair;
private OAuthAuthorizationServer authServer;

/**
 * Produce the JWT key pair
 */
@Produces
@Singleton
public KeyPair produceKeyPair() {
if (keyPair == null) {
try {
File keystoreDir = new File(keystorePath);
keystoreDir.mkdirs();

SimpleKeyStore keyStore = new SimpleKeyStore(keystoreDir);
keyPair = keyStore.load();

if (keyPair == null) {
keyPair = keyStore.keys();
keyStore.save(keyPair);
log.info("oauth.keygen.created: {}", keystorePath);
} else {
log.info("oauth.keygen.loaded: {}", keystorePath);
}
} catch (Exception e) {
log.error("oauth.keygen.error: {}", e.getMessage(), e);
throw new RuntimeException("Failed to initialize OAuth keypair", e);
}
}
return keyPair;
}

/**
 * Produce the OAuth Token Factory
 */
@Produces
@Singleton
public OAuthTokenFactory produceTokenFactory() {
JWTGen jwtGen = new JWTGen();
KeyPair kp = produceKeyPair();
return new OAuthTokenFactory(jwtGen, kp, issuer, tokenDurationSeconds, "default-key");
}

/**
 * Produce the JTI Revocation Store
 */
@Produces
@Singleton
public JTIRevocationStore produceRevocationStore() {
return new JTIRevocationStore(revocationMaxEntries, revocationTtl);
}

/**
 * Produce the Device Code Store
 */
@Produces
@Singleton
public DeviceCodeStore produceDeviceCodeStore() {
return new DeviceCodeStore(10000, deviceCodeTtl, deviceCodeInterval);
}

/**
 * Produce the Token Validator
 */
@Produces
@Singleton
public OAuthTokenValidator produceTokenValidator() {
Set<String> trustedIssuers = new HashSet<>();
trustedIssuers.add(issuer);

return new OAuthTokenValidator(
produceKeyPair(),
produceRevocationStore(),
trustedIssuers,
clockSkewSeconds
);
}

/**
 * Produce the Client Registry
 */
@Produces
@Singleton
public ClientRegistry produceClientRegistry() {
ClientRegistry registry = new ClientRegistry();

// Register default CLI client for device flow
ClientRegistry.OAuthClient cliClient = new ClientRegistry.OAuthClient(
"iq-cli",
null, // public client
new HashSet<>(Arrays.asList(
"http://localhost:8080/callback",
"urn:ietf:wg:oauth:2.0:oob" // out-of-band for headless
)),
new HashSet<>(Arrays.asList(
"urn:ietf:params:oauth:grant-type:device_code",
"refresh_token"
)),
new HashSet<>(Arrays.asList(
"chat.read", "chat.write", "agent.trigger",
"sparql.select", "sparql.update",
"connector.execute",
"control.read", "control.write"
)),
"IQ CLI",
true // public
);
registry.register(cliClient);

// Register default MCP client for device flow
ClientRegistry.OAuthClient mcpClient = new ClientRegistry.OAuthClient(
"iq-mcp",
null, // public client
new HashSet<>(Collections.singletonList("urn:ietf:wg:oauth:2.0:oob")),
new HashSet<>(Arrays.asList(
"urn:ietf:params:oauth:grant-type:device_code",
"refresh_token"
)),
new HashSet<>(Arrays.asList(
"chat.read", "chat.write",
"sparql.select", "sparql.update"
)),
"IQ MCP Server",
true // public
);
registry.register(mcpClient);

log.info("oauth.clients.registered: cli, mcp");
return registry;
}

/**
 * Produce the OAuth Authorization Server
 */
@Produces
@Singleton
public OAuthAuthorizationServer produceOAuthAuthorizationServer() {
if (authServer == null) {
authServer = new OAuthAuthorizationServer(
produceTokenFactory(),
produceTokenValidator(),
produceClientRegistry(),
produceRevocationStore(),
produceDeviceCodeStore()
);
log.info("oauth.server.initialized: {}", issuer);
}
return authServer;
}
}
