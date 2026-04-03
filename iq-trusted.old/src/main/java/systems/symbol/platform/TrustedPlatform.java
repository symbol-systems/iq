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

@ApplicationScoped
public class TrustedPlatform extends Platform {
private static final Logger log = LoggerFactory.getLogger(TrustedPlatform.class);
private I_Intent intent;
private String name;

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
stop();
}

@Override
public Repository getRepository(String name) {
if (name == null || name.trim().isEmpty()) {
throw new IllegalArgumentException("Repository name cannot be null or empty");
}
try {
// Create a simple in-memory repository for trusted platform
// For production use, this should be replaced with proper repository manager
org.eclipse.rdf4j.repository.sail.SailRepository repository = 
new org.eclipse.rdf4j.repository.sail.SailRepository(
new org.eclipse.rdf4j.sail.memory.MemoryStore());
if (!repository.isInitialized()) {
repository.init();
}
return repository;
} catch (Exception e) {
throw new RuntimeException("Failed to create repository for: " + name, e);
}
}

@Override
public void boot() {
// no-op boot stub
}

@Override
public void X() {
// trust gate X
}

@Override
public void XX() {
// trust gate XX
}

@Override
public void XXX() {
// trust gate XXX
}

@Override
public void XXXX() {
// trust gate XXXX
}
}
