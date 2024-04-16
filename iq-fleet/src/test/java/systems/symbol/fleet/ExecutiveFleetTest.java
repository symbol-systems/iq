package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.I_Agent;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.iq.LiveModel;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.string.Validate;

import java.io.File;
import java.io.IOException;

class ExecutiveFleetTest {

    static BootstrapRepository assets;
    private static IRI self;
    String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new BootstrapRepository();
        self = assets.load(new File("src/test/resources/fleet"), COMMONS.IQ_NS_TEST);
        assert COMMONS.IQ_NS_TEST.equals( self.stringValue() );
    }

    @Test
    void deployFleet() throws Exception {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.out.println("exec.fleet.llm.skipped: ");
            return;
        }
        System.out.println("exec.fleet.deploy: "+self);
        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            EnvsAsSecrets secrets = new EnvsAsSecrets();
            ChatGPT gpt = new ChatGPT(OPENAI_API_KEY, 1000);
            ExecutiveFleet fleet = new ExecutiveFleet(self, gpt, model, secrets);

            for(I_Agent agent: fleet.getAgents()) {
                System.out.println("exec.fleet.deployed: " + agent.getSelf() + " @ "+agent.getStateMachine().getState());
            }
            fleet.start();
            fleet.stop();
//            System.out.println("fleet.dump");
//            RDFDump.dump(model);
        }
    }

}