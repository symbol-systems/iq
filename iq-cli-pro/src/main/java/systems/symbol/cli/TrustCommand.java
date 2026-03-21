package systems.symbol.cli;

import picocli.CommandLine;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import systems.symbol.platform.I_Self;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;
import java.time.Instant;

@CommandLine.Command(name = "trust", description = "Export this "+ I_Self.CODENAME +" to another.")
public class TrustCommand extends AbstractCLICommand{

@CommandLine.Parameters(index = "0", description = "The identity of the agent you want to trust or action (me|list|revoke)", defaultValue = "me")
String identity = "me";

@CommandLine.Option(names = {"--no-commit"}, description = "Don't commit changes to repository")
boolean noCommit = false;

public TrustCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (!context.isInitialized()) {
System.out.println("iq.trust.failed: not initialized");
return "uninitialized";
}

IRI self = context.getSelf();
System.out.println("iq.trust: self=" + self.getLocalName() + ", target=" + identity);

if ("me".equalsIgnoreCase(identity)) {
trustSelf();
} else if ("list".equalsIgnoreCase(identity)) {
listTrusts();
} else if ("revoke".equalsIgnoreCase(identity)) {
System.out.println("iq.trust.revoke: please specify IRI to revoke");
return "revoke:pending";
} else {
trustRemote(identity);
}

return "trusted:" + identity;
}

private void trustSelf() {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();

// Use the proper IQ_NS.TRUSTS predicate
iq.getConnection().add(self, IQ_NS.TRUSTS, self);

// Add timestamp
Instant now = Instant.now();
iq.getConnection().add(self, DCTERMS.MODIFIED, 
IQStore.vf.createLiteral(now));

if (!noCommit) {
iq.getConnection().commit();
}
log.info("iq.trust.self: {} trusts itself", self);
System.out.println("  ✓ Trust self: " + self.getLocalName());
} catch (Exception e) {
log.error("iq.trust.self.failed: {}", e.getMessage(), e);
System.out.println("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

private void trustRemote(String identity) {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();
IRI target = IQStore.vf.createIRI(identity);

iq.getConnection().add(self, IQ_NS.TRUSTS, target);

// Add timestamp
Instant now = Instant.now();
iq.getConnection().add(self, DCTERMS.MODIFIED, 
IQStore.vf.createLiteral(now));

if (!noCommit) {
iq.getConnection().commit();
}

log.info("iq.trust.remote: {} trusts {}", self, target);
System.out.println("  ✓ Trust arc: " + self.getLocalName() + " -> " + target.getLocalName());
} catch (Exception e) {
log.error("iq.trust.remote.failed: {}", e.getMessage(), e);
System.out.println("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

private void listTrusts() {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();

System.out.println("  Trusts for " + self.getLocalName() + ":");

var stmts = iq.getConnection().getStatements(self, IQ_NS.TRUSTS, null);
stmts.forEach(stmt -> {
System.out.println("- " + ((IRI)stmt.getObject()).getLocalName());
});

log.info("iq.trust.list: {} has trust arcs", self);
} catch (Exception e) {
log.error("iq.trust.list.failed: {}", e.getMessage(), e);
System.out.println("  ✗ Error: " + e.getMessage());
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}
}
