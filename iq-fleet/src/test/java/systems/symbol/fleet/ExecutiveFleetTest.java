package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.intent.Learn;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.COMMONS;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.LiveModel;
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
            System.err.println("exec.fleet.llm.skipped: ");
            return;
        }
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        ChatGPT gpt = new ChatGPT(OPENAI_API_KEY, 1000);
        System.out.println("exec.fleet.deploy: "+self);

        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            ExecutiveFleet fleet = new ExecutiveFleet(self, gpt, model, secrets);
            fleet.start();
            assert !fleet.agents.isEmpty();
            fleet.stop();
            Iterable<Statement> statements = model.getStatements(self, Learn.KNOWS, Values.iri(COMMONS.IQ_NS_TEST, "NaturalLanguage"));
            boolean hasNext = statements.iterator().hasNext();
            System.out.println("fleet.done: "+hasNext+" x "+fleet.agents.size());
//            assert hasNext;
//            RDFDump.dump(model);

        }
    }

}