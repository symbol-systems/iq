package systems.symbol.fleet;

import com.google.gson.Gson;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import systems.symbol.COMMONS;
import systems.symbol.agent.MyFacade;
import systems.symbol.agent.tools.APIException;
import systems.symbol.finder.Recommends;
import systems.symbol.intent.Learn;
import systems.symbol.intent.Remodel;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.SimpleBindings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

class ExecutiveFleetTest {
    DynamicModelFactory dmf = new DynamicModelFactory();
    static BootstrapRepository assets;
    private static IRI self;
    String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    Gson gson = new Gson();
    private final Resource search = Values.iri(COMMONS.IQ_NS_TEST, "search");
    private static final File testedFolder = new File("tested/");

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new BootstrapRepository();
        self = assets.load(new File("src/test/resources/fleet"), COMMONS.IQ_NS_TEST);
        assert COMMONS.IQ_NS_TEST.equals( self.stringValue() );
        testedFolder.mkdirs();
    }

    @Test
    void deployHealthy() throws Exception, APIException {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("exec.fleet.llm.skipped: ");
            return;
        }
        ChatGPT gpt = new ChatGPT(OPENAI_API_KEY, 1000);
        System.out.println("exec.fleet: "+self);

        APISecrets secrets = new APISecrets(new EnvsAsSecrets());

        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            SimpleBindings my = new SimpleBindings();
            my.put(MyFacade.PROMPT, "how are you?");

            ExecutiveFleet fleet = new ExecutiveFleet(self, model, secrets, gpt, my);
            fleet.start();
            assert !fleet.agents.isEmpty();
            fleet.stop();

            //            RDFDump.dump(model);
            Iterable<Statement> statements = model.getStatements(self, Learn.KNOWS, Values.iri(COMMONS.IQ_NS_TEST, "Self"));
            boolean hasNext = statements.iterator().hasNext();
            System.out.println("fleet.done: "+hasNext+" x "+fleet.agents.size());
            System.out.println("fleet.my: "+gson.toJson(my));
            assert hasNext;
            assert my.containsKey("results");
            assert my.get("results") instanceof List;
            assert ((List<Map<?,?>>)my.get("results")).get(0).get("label").equals("healthy");
        }
    }

    @Test
    void searchBrave() throws Exception, APIException {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("brave.fleet.llm.skipped: ");
            return;
        }
        ChatGPT gpt = new ChatGPT(OPENAI_API_KEY, 1000);
        System.out.println("brave.fleet: "+self);

        APISecrets secrets = new APISecrets(new EnvsAsSecrets());
        secrets.grant("https://api.search.brave.com", "BRAVE_API_KEY");

        Stopwatch stopwatch = new Stopwatch();
        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            SimpleBindings my = new SimpleBindings();
            DynamicModel memoryModel = dmf.createEmptyModel();
            ExecutiveFleet fleet = new ExecutiveFleet(self, model, secrets, gpt, my);

            fleet.intents.add(new Remodel(self, memoryModel, new ModelScriptCatalog(model)));
            fleet.deploy();
            assert !fleet.agents.isEmpty();

            String prompt = "What is OpenAI mission?";
            my.put(MyFacade.PROMPT, prompt);
            System.out.println("brave.deployed @ "+stopwatch.summary());
            fleet.getAgent(self).getStateMachine().transition(search);

            fleet.stop();

            Map<Resource, Double> similarity = Recommends.similarity(memoryModel, Values.iri("http://schema.org/description"), prompt, 0.6);
            System.out.println("brave.recommends @ "+stopwatch.summary());
            System.out.println("brave.similarity: "+similarity);
            System.out.println("brave.found: "+memoryModel.size());
            assert memoryModel.size()>10;

            Recommends.score(memoryModel, similarity);
            File dumpFile = new File(testedFolder, "brave.search.ttl");
            RDFDump.dump(memoryModel, Files.newOutputStream(dumpFile.toPath()), RDFFormat.TURTLE);

            Model pruned = Recommends.prune(memoryModel, similarity);
            File prunedFile = new File(testedFolder, "brave.search.pruned.ttl");
            RDFDump.dump(pruned, Files.newOutputStream(prunedFile.toPath()), RDFFormat.TURTLE);
            System.out.println("brave.dumpFile @ "+stopwatch.summary());
        }
    }
}