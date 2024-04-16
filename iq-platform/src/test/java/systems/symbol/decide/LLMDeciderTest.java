package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.agent.LazyAgent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.iq.LiveModel;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.string.Validate;

import java.io.File;
import java.io.IOException;

class LLMDeciderTest {

    private static BootstrapRepository assets;
    private static IRI self;
    String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    IRI consult = Values.iri(COMMONS.IQ_NS_TEST + "consult");
    IRI chitchat = Values.iri(COMMONS.IQ_NS_TEST +"chit_chat");

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new BootstrapRepository();
        self = assets.load(new File("src/test/resources/chat"), COMMONS.IQ_NS_TEST);
    }

    @Test
    void decide() throws Exception, APIException {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.out.println("decide.llm.skipped: ");
            return;
        }

        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);

            System.out.println("decide.models: " + model.size());
            RDFDump.dump(model, System.out, RDFFormat.TURTLE);
            assert model.size() > 1;

            LazyAgent agent = new LazyAgent(model, self);
            LLMDecision decider = new LLMDecision(new ChatGPT(OPENAI_API_KEY, 1000), agent);

            assert chitchat.equals(agent.getStateMachine().getState());
            decider.prompt("help me decide");
            Resource decision = decider.decide();
            assert decision != null;
            System.out.println("decide.decision: " + decision);
            assert consult.equals(decision);
        }
    }
}