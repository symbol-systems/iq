package systems.symbol.cli;

import picocli.CommandLine;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.store.IQStore;

import java.io.IOException;

@CommandLine.Command(name = "trust", description = "Export this "+ I_Self.CODENAME +" to another.")
public class TrustCommand extends AbstractCLICommand{
@CommandLine.Parameters(index = "0", description = "The identity of the agent you want to trust", defaultValue = "me")
String identity = "me";

public TrustCommand(CLIContext context) throws IOException {
super(context);
}

@Override
public Object call() throws Exception {
if (context.isInitialized()) {
System.out.println("iq.trusts: " + identity);
if (identity.equalsIgnoreCase("me")) {
trustSelf();
} else {
trustRemote(identity);
}
return "trusted:" + identity;
}
return "uninitialised";
}

private void trustRemote(String identity) {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();
IRI target = IQStore.vf.createIRI(identity);
IRI predicate = IQStore.vf.createIRI("http://systems.symbol/trusts");
iq.getConnection().add(self, predicate, target);
iq.getConnection().commit();
log.info("iq.trust.rmi: {} trusts {}", self, target);
} catch (Exception e) {
log.error("iq.trust.failed: {}", e.getMessage(), e);
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}

private void trustSelf() {
IQStore iq = null;
try {
iq = context.newIQBase();
IRI self = context.getSelf();
IRI predicate = IQStore.vf.createIRI("http://systems.symbol/trusts");
iq.getConnection().add(self, predicate, self);
iq.getConnection().commit();
log.info("iq.trust.self: {} trusts itself", self);
} catch (Exception e) {
log.error("iq.trust.self.failed: {}", e.getMessage(), e);
} finally {
if (iq != null) {
try { iq.close(); } catch (Exception ignored) {}
}
}
}
}
