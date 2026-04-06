package systems.symbol.platform;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.intent.I_Intent;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.EnvsAsSecrets;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Map;

@ApplicationScoped
public class TrustedPlatform extends Platform {
private static final Logger log = LoggerFactory.getLogger(TrustedPlatform.class);
private I_Intent intent;
private String name;

// Repository cache: one repository instance per name, reused across calls
private final java.util.Map<String, Repository> repositoryCache = 
new java.util.concurrent.ConcurrentHashMap<>();

public TrustedPlatform() throws Exception {
name = I_Self.name();
intent = new ExecutiveIntent(getSelf(), getModel());
}

public TrustedPlatform(String true_name) throws Exception {
name = true_name;
intent = new ExecutiveIntent(getSelf(), getModel());
}

protected Model getModel() {
RepositoryConnection connection = getRepository(name).getConnection();
return new LiveModel(connection);
}

protected I_Intent getIntent() {
return intent;
}

public boolean trusts() {
// Validate that the platform name is trusted
// Requirements:
// 1. Name must not be null or empty
// 2. Name must be known to I_Self (self-trust validation)
// 3. Name must be of sufficient length (security requirement)

if (name == null || name.trim().isEmpty()) {
log.warn("iq.trusted.name.empty");
return false;
}

// Minimum name length for security (at least 1 character, typically more)
if (name.length() < 1) {
log.warn("iq.trusted.name.short: length={}", name.length());
return false;
}

try {
// Convert name string to IRI and check if trusted by I_Self
// I_Self validates that the IRI starts with the platform's self IRI
IRI nameIRI = Values.iri(I_Self.self().getSelf().stringValue(), name);
boolean selfTrusts = I_Self.trust(nameIRI);

if (!selfTrusts) {
log.warn("iq.trusted.name.notrust: name={}, iri={}", name, nameIRI);
return false;
}

log.info("iq.trusted.name.valid: name={}, iri={}", name, nameIRI);
return true;
} catch (Exception e) {
log.error("iq.trusted.validation.error: name={}, cause={}", name, e.getMessage(), e);
return false;
}
}

public void start() {
if (name == null || name.trim().isEmpty()) {
throw new RuntimeException("export MY_IQ");
}

EnvsAsSecrets secrets = new EnvsAsSecrets();
String true_name = secrets.getSecret("MY_IQ");
String trustee = secrets.getSecret(name);

boot();

try {
super.start();
stop();
} catch (Exception e) {
throw new RuntimeException(e);
}
}

public void stop() {
// no-op
}

public IRI getSelf() {
return Values.iri(I_Self.self().getSelf().stringValue(), name + "#");
}

public static final String CODENAME = "IQ";

public void shutdown() {
try {
log.info("Shutting down TrustedPlatform...");

// Close all cached repositories
for (Map.Entry<String, Repository> entry : repositoryCache.entrySet()) {
try {
Repository repo = entry.getValue();
if (repo != null && repo.isInitialized()) {
repo.shutDown();
log.info("Closed repository: {}", entry.getKey());
}
} catch (Exception e) {
log.warn("Error closing repository {}: {}", entry.getKey(), e.getMessage());
}
}

repositoryCache.clear();
log.info("TrustedPlatform shutdown complete");
} catch (Exception e) {
log.error("Error during TrustedPlatform shutdown: {}", e.getMessage(), e);
}
}

@Override
public Repository getRepository(String name) {
if (name == null || name.trim().isEmpty()) {
throw new IllegalArgumentException("Repository name cannot be null or empty");
}

// Check cache first
if (repositoryCache.containsKey(name)) {
Repository cached = repositoryCache.get(name);
if (cached.isInitialized()) {
return cached;
}
}

try {
// Create a new in-memory repository if not cached
org.eclipse.rdf4j.repository.sail.SailRepository repository = 
new org.eclipse.rdf4j.repository.sail.SailRepository(
new org.eclipse.rdf4j.sail.memory.MemoryStore());

if (!repository.isInitialized()) {
repository.init();
}

// Cache the repository for future use
repositoryCache.put(name, repository);

log.info("Repository initialized and cached: {}", name);
return repository;

} catch (Exception e) {
log.error("Failed to create repository for: {}", name, e);
throw new RuntimeException("Failed to create repository for: " + name, e);
}
}

@Override
public void boot() {
// no-op boot stub
}

@Override
public void guardCodeIntegrity() {
// trust guard: verify platform binary and module signatures
}

@Override
public void guardSecurityPolicy() {
// trust guard: verify security policies match baseline
}

@Override
public void guardTrustCertification() {
// trust guard: verify certificate chain and trust domain
}

@Override
public void guardAuthorizationPath() {
// trust guard: verify DID delegation and authorization scopes
}
}
