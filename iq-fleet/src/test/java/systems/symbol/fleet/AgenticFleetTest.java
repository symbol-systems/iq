package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.I_Agent;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.iq.LiveModel;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.secrets.EnvsAsSecrets;

import java.io.File;
import java.io.IOException;

class AgenticFleetTest {

    static BootstrapRepository assets;
    private static IRI fleet;

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new BootstrapRepository();
        fleet = assets.load(new File("src/test/resources/fleet"), COMMONS.IQ_NS_TEST);
        assert COMMONS.IQ_NS_TEST.equals( fleet.stringValue());
    }

    @Test
    void deployFleet() throws Exception {
        System.out.println("fleet.deploy: "+fleet);
        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            EnvsAsSecrets secrets = new EnvsAsSecrets();

            AgenticFleet fleet = new AgenticFleet(model, secrets);

            for(I_Agent agent: fleet.getAgents()) {
                System.out.println("fleet.deployed: " + agent.getSelf() + " @ "+agent.getStateMachine().getState());
            }
            fleet.start();
            RDFDump.dump(model);
            fleet.stop();
        }
    }
}