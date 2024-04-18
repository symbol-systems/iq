package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.LiveModel;

import java.io.File;
import java.io.IOException;

class AgenticFleetTest {

static BootstrapRepository assets;
private static IRI self;

@BeforeAll
public static void setUp() throws IOException {
assets = new BootstrapRepository();
self = assets.load(new File("src/test/resources/fleet"), COMMONS.IQ_NS_TEST);
assert COMMONS.IQ_NS_TEST.equals( self.stringValue());
}

@Test
void deployFleet() throws Exception {
try (RepositoryConnection connection = assets.getConnection()) {
Model model = new LiveModel(connection);
AgenticFleet fleet = new AgenticFleet(self, model, null);
fleet.start();
assert !fleet.agents.isEmpty();
System.out.println("agent.fleet: "+ self+" x "+fleet.agents.size());
fleet.stop();
}
}
}