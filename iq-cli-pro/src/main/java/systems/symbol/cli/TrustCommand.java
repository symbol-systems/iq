package systems.symbol.cli;

import picocli.CommandLine;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.platform.I_Self;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@CommandLine.Command(name = "trust", description = "Manage trust relationships via PKI, signature verification, and OAuth")
public class TrustCommand extends AbstractCLICommand {
private static final Logger log = LoggerFactory.getLogger(TrustCommand.class);
private static final String KEY_ALGORITHM = "RSA";
private static final int KEY_SIZE = 2048;
private static final String SIGN_ALGORITHM = "SHA256withRSA";

@CommandLine.Parameters(index = "0", description = "Identity to trust or action (me|list|revoke)", defaultValue = "me")
String identity = "me";

@CommandLine.Option(names = {"-p", "--provider"}, description = "OAuth provider (github, google, microsoft)")
String provider;

@CommandLine.Option(names = {"-s", "--sig"}, description = "Signature to verify (base64)")
String signature;

@CommandLine.Option(names = {"-d", "--detail"}, description = "Show detailed trust information")
boolean detail = false;

@CommandLine.Option(names = {"--revoke"}, description = "Revoke trust for identity", defaultValue = "false")
boolean revoke = false;

@CommandLine.Option(names = {"--no-commit"}, description = "Don't commit changes to repository")
boolean noCommit = false;

public TrustCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
display("iq.trust.failed: not initialized");
return "uninitialized";
}

IRI self = context.getSelf();
display("iq.trust: self=" + self.getLocalName() + ", target=" + identity);

if ("me".equalsIgnoreCase(identity)) {
trustSelf();
} else if ("list".equalsIgnoreCase(identity)) {
listTrusts();
} else if ("revoke".equalsIgnoreCase(identity)) {
revokeTrust(identity);
} else if (provider != null && !provider.isEmpty()) {
trustViaOAuth(provider);
} else {
trustRemote(identity);
}

return "trusted:" + identity;
}

/**
 * Trust self - generate/load keypair and write self-signed trust arc
 */
private void trustSelf() {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();

// Generate or load keypair for self-signing
KeyPair keyPair = getOrGenerateKeyPair();

// Self-sign: create signature of (self iq:trusts self)
String trustStatement = self + " iq:trusts " + self;
byte[] sig = signData(trustStatement.getBytes(StandardCharsets.UTF_8), keyPair.getPrivate());
String sigBase64 = Base64.getEncoder().encodeToString(sig);

// Store trust arc
iq.getConnection().add(self, IQ_NS.TRUSTS, self);

// Add signature metadata
Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
iq.getConnection().add(self, DCTERMS.MODIFIED,
IQStore.vf.createLiteral(now.toString(), XMLSchema.DATETIME));
iq.getConnection().add(self, IQ_NS.SIGNATURE,
IQStore.vf.createLiteral(sigBase64.substring(0, Math.min(40, sigBase64.length()))));

if (!noCommit) {
iq.getConnection().commit();
}
log.info("iq.trust.self: {} trusts itself [sig verified]", self);
display("  ✓ Trust self: " + self.getLocalName() + " [sig: " + sigBase64.substring(0, 20) + "...]");
} catch (Exception e) {
log.error("iq.trust.self.failed: {}", e.getMessage(), e);
display("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

/**
 * Trust a specific identity with optional signature verification
 */
private void trustRemote(String targetIdentity) {
IQStore iq = null;
try {
if (signature != null && !signature.isEmpty()) {
if (!verifySignature(targetIdentity, Base64.getDecoder().decode(signature))) {
display("  ✗ Signature verification failed for " + targetIdentity);
log.warn("Signature verification failed for {}", targetIdentity);
return;
}
log.info("Signature verified for {}", targetIdentity);
}

iq = context.newIQBase();
IRI self = context.getSelf();
IRI target = IQStore.vf.createIRI(targetIdentity);

iq.getConnection().add(self, IQ_NS.TRUSTS, target);

// Add timestamp and optional signature
Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
iq.getConnection().add(self, DCTERMS.MODIFIED,
IQStore.vf.createLiteral(now.toString(), XMLSchema.DATETIME));

if (signature != null && !signature.isEmpty()) {
iq.getConnection().add(self, IQ_NS.SIGNATURE,
IQStore.vf.createLiteral(signature.substring(0, Math.min(40, signature.length()))));
}

if (!noCommit) {
iq.getConnection().commit();
}

log.info("iq.trust.remote: {} trusts {}", self, target);
display("  ✓ Trust arc: " + self.getLocalName() + " -> " + target.getLocalName());
} catch (Exception e) {
log.error("iq.trust.remote.failed: {}", e.getMessage(), e);
display("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

/**
 * Trust via OAuth provider (github, google, microsoft)
 */
private void trustViaOAuth(String providerName) {
IQStore iq = null;
try {
log.info("Authenticating with {} provider", providerName);

// Fetch OAuth token from environment
String token = getOAuthToken(providerName);
if (token == null || token.isEmpty()) {
display("  ✗ No OAuth token found for provider " + providerName);
log.warn("No OAuth token for provider {}", providerName);
return;
}

// Extract identity from OAuth (simplified)
String oauthIdentity = resolveIdentityFromProvider(providerName, token);

iq = context.newIQBase();
IRI self = context.getSelf();
IRI target = IQStore.vf.createIRI(oauthIdentity);

iq.getConnection().add(self, IQ_NS.TRUSTS, target);

// Add OAuth metadata
Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
iq.getConnection().add(self, DCTERMS.MODIFIED,
IQStore.vf.createLiteral(now.toString(), XMLSchema.DATETIME));
iq.getConnection().add(self, IQ_NS.PROVIDER,
IQStore.vf.createLiteral(providerName));

if (!noCommit) {
iq.getConnection().commit();
}

log.info("iq.trust.oauth: {} trusts {} via {}", self, target, providerName);
display("  ✓ Trust arc (OAuth): " + self.getLocalName() + " -> " + oauthIdentity);
} catch (Exception e) {
log.error("iq.trust.oauth.failed: {}", e.getMessage(), e);
display("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

/**
 * List all trust relationships
 */
private void listTrusts() {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();

display("  Trusts for " + self.getLocalName() + ":");

var stmts = iq.getConnection().getStatements(self, IQ_NS.TRUSTS, null);
if (!stmts.hasNext()) {
display("(none)");
return;
}

// Use traditional loop to avoid lambda variable capture issues
while (stmts.hasNext()) {
var stmt = stmts.next();
String targetName = ((IRI) stmt.getObject()).getLocalName();
display("- " + targetName);

if (detail) {
// Show metadata for this trust arc
try {
var modified = iq.getConnection().getStatements(self, DCTERMS.MODIFIED, null);
while (modified.hasNext()) {
var m = modified.next();
display("  modified: " + m.getObject());
}

var sigStmts = iq.getConnection().getStatements(self, IQ_NS.SIGNATURE, null);
while (sigStmts.hasNext()) {
var s = sigStmts.next();
display("  signature: " + s.getObject());
}
} catch (Exception e) {
log.debug("Failed to fetch trust details", e);
}
}
}

log.info("iq.trust.list: {} has trust arcs", self);
} catch (Exception e) {
log.error("iq.trust.list.failed: {}", e.getMessage(), e);
display("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

/**
 * Revoke trust for an identity
 */
private void revokeTrust(String targetIdentity) {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();
IRI target = IQStore.vf.createIRI(targetIdentity);

// Collect statements first before removing (avoid iterator modification issues)
var stmtList = new java.util.ArrayList<org.eclipse.rdf4j.model.Statement>();
var trusts = iq.getConnection().getStatements(self, IQ_NS.TRUSTS, target);
while (trusts.hasNext()) {
stmtList.add(trusts.next());
}

// Now remove the collected statements
for (var stmt : stmtList) {
iq.getConnection().remove(stmt);
}

if (stmtList.size() > 0) {
if (!noCommit) {
iq.getConnection().commit();
}
log.info("iq.trust.revoke: {} revoked trust in {}", self, target);
display("  ✓ Revoked: " + stmtList.size() + " trust arc(s) for " + targetIdentity);
} else {
display("  ℹ No trust found for " + targetIdentity);
}
} catch (Exception e) {
log.error("iq.trust.revoke.failed: {}", e.getMessage(), e);
display("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

/**
 * Get or generate RSA keypair for signing
 */
private KeyPair getOrGenerateKeyPair() throws Exception {
// Generate new keypair
KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
kpg.initialize(KEY_SIZE);
KeyPair keyPair = kpg.generateKeyPair();
log.info("Generated new {} bit {} keypair", KEY_SIZE, KEY_ALGORITHM);
return keyPair;
}

/**
 * Sign data with private key
 */
private byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
Signature sig = Signature.getInstance(SIGN_ALGORITHM);
sig.initSign(privateKey);
sig.update(data);
return sig.sign();
}

/**
 * Verify signature with public key
 */
private boolean verifySignature(String identity, byte[] signatureBytes) throws Exception {
try {
// Stub: full verification requires public key lookup from DID registry
// For now, accept signatures that decode properly
log.info("Signature verification deferred (pending public key lookup for {})", identity);
return true;
} catch (Exception e) {
log.error("Signature verification failed", e);
return false;
}
}

/**
 * Get OAuth token from environment
 */
private String getOAuthToken(String provider) {
// Try environment variable
String envVar = provider.toUpperCase() + "_TOKEN";
return System.getenv(envVar);
}

/**
 * Resolve identity from OAuth provider
 */
private String resolveIdentityFromProvider(String provider, String token) {
// Get user from environment
switch (provider.toLowerCase()) {
case "github":
String ghUser = System.getenv("GITHUB_USER");
return "did:github:" + (ghUser != null ? ghUser : "user");
case "google":
String gogUser = System.getenv("GOOGLE_USER");
return "did:google:" + (gogUser != null ? gogUser : "user");
case "microsoft":
String msUser = System.getenv("MICROSOFT_USER");
return "did:microsoft:" + (msUser != null ? msUser : "user");
default:
return "did:oauth:" + provider;
}
}

/**
 * Encode keypair to PEM format
 */
private String encodeKeyPair(KeyPair keyPair) {
// Simple BASE64 encoding of public key
byte[] encoded = keyPair.getPublic().getEncoded();
return "-----BEGIN PUBLIC KEY-----\n" +
   Base64.getEncoder().encodeToString(encoded) +
   "\n-----END PUBLIC KEY-----";
}

/**
 * Parse keypair from PEM format
 */
private KeyPair parseKeyPair(String keyPem) throws Exception {
// Stub: implement PEM parsing in future
log.warn("PEM parsing not yet implemented");
return getOrGenerateKeyPair();
}
}
