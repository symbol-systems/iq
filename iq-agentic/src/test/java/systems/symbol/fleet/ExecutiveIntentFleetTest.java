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

import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_Agentic;
import systems.symbol.finder.Recommends;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Remodel;
import systems.symbol.intent.Think;
import systems.symbol.lake.BootstrapRepository;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.llm.gpt.LLMFactory;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.APISecrets;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static systems.symbol.platform.IQ_NS.KNOWS;
import static systems.symbol.platform.IQ_NS.TEST;

class ExecutiveIntentFleetTest {
    private static APISecrets secrets;
    DynamicModelFactory dmf = new DynamicModelFactory();
    static BootstrapRepository assets;
    private static IRI self;
    String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    Gson gson = new Gson();
    private static final Resource selfIntent = Values.iri(TEST, "self");
    private static final File testedFolder = new File("tested/");
    private static final Resource aware = Values.iri(TEST, "aware");
    private static final Resource think = Values.iri(TEST, "think");

    @BeforeAll
    public static void setUp() throws IOException {
        assets = new BootstrapRepository();
        self = assets.load(new File("src/test/resources/fleet"), TEST);
        assert TEST.equals(self.stringValue());
        testedFolder.mkdirs();
        secrets = new APISecrets(new EnvsAsSecrets());
        secrets.grant("https://api.search.brave.com", "BRAVE_API_KEY");
        secrets.grant("https://api.openai.com/", "OPENAI_API_KEY");
    }

    // @Test
    @SuppressWarnings("unchecked")
    void fleetSelfTest() throws Exception {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("healthy.fleet.llm.skipped: ");
            return;
        }
        System.out.println("---- self");
        GenericGPT gpt = new GenericGPT(OPENAI_API_KEY, LLMFactory.GPT3_5_Turbo(1000));

        APISecrets secrets = new APISecrets(new EnvsAsSecrets());

        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            ExecutiveFleet fleet = new ExecutiveFleet(self, model, model, secrets, gpt);
            fleet.deploy();
            assert !fleet.agents.isEmpty();

            I_Agentic<String> context = fleet.getContext(self);
            assert null != context;
            Bindings my = context.getBindings();

            fleet.start();
            System.out.println("healthy.fleet.prompting: " + self);
            context.getConversation().user("How are you ?");
            fleet.run();
            System.out.println("healthy.fleet.done: " + gson.toJson(context.getBindings()));
            fleet.stop();
            System.out.println("healthy.fleet.stopped");

            Iterable<Statement> statements = model.getStatements(self, KNOWS, Values.iri(TEST, "Self"));
            boolean hasNext = statements.iterator().hasNext();
            System.out.println("healthy.fleet.done: " + hasNext + " x " + fleet.agents.size());
            assert hasNext;
            assert my.containsKey("results");
            assert my.get("results") instanceof List;
            assert ((List<Map<?, ?>>) my.get("results")).get(0).get("label").equals("healthy");
        }
    }

    // @Test
    void searchBrave() throws Exception {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("brave.fleet.llm.skipped: ");
            return;
        }
        System.out.println("---- search");
        GenericGPT gpt = new GenericGPT(OPENAI_API_KEY, LLMFactory.GPT3_5_Turbo(1000));
        System.out.println("brave.fleet: " + self);

        Stopwatch stopwatch = new Stopwatch();
        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            DynamicModel memoryModel = dmf.createEmptyModel();
            ExecutiveFleet fleet = new ExecutiveFleet(self, model, model, secrets, gpt);

            fleet.intents.add(new Remodel(self, memoryModel, new ModelScriptCatalog(model)));
            fleet.deploy();
            assert !fleet.agents.isEmpty();

            I_Agentic<String> context = fleet.getContext(self);
            assert null != context;
            I_Agent agent = fleet.getAgent(self);
            assert null != agent;
            assert null != context.getConversation();

            String prompt = "What is OpenAI mission?";
            context.getConversation().user(prompt);

            System.out.println("brave.thread.in: @ " + stopwatch.summary());
            fleet.start();
            fleet.run();
            System.out.println("brave.thread.out: @ " + stopwatch.summary());
            fleet.stop();

            Map<Resource, Double> similarity = Recommends.similarity(memoryModel,
                    Values.iri("http://schema.org/description"), prompt, 0.6);
            System.out.println("brave.recommends @ " + stopwatch.summary());
            System.out.println("brave.similarity: " + similarity);
            System.out.println("brave.found: " + memoryModel.size());
            assert memoryModel.size() > 10;

            Recommends.score(memoryModel, similarity);
            File dumpFile = new File(testedFolder, "brave.search.ttl");
            RDFDump.dump(memoryModel, Files.newOutputStream(dumpFile.toPath()), RDFFormat.TURTLE);

            Model pruned = Recommends.prune(memoryModel, similarity);
            File prunedFile = new File(testedFolder, "brave.search.pruned.ttl");
            RDFDump.dump(pruned, Files.newOutputStream(prunedFile.toPath()), RDFFormat.TURTLE);
            System.out.println("brave.dumpFile @ " + stopwatch.summary());
        }
    }

    // @Test
    void guardedFleet() throws Exception {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("guarded.llm.skipped: ");
            return;
        }
        System.out.println("---- guarded");
        GenericGPT gpt = new GenericGPT(OPENAI_API_KEY, LLMFactory.GPT3_5_Turbo(1000));
        System.out.println("guarded.fleet: " + self);

        APISecrets secrets = new APISecrets(new EnvsAsSecrets());

        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);

            ExecutiveFleet fleet = new ExecutiveFleet(self, model, model, secrets, gpt);
            fleet.deploy();
            I_Agent agent = fleet.getAgent(self);
            assert null != agent;

            // attempt a guarded state
            boolean[] guarded = { false };
            try {
                Resource transitioned = agent.getStateMachine().transition(aware);
                System.out.println("fleet.unguarded: " + transitioned);
            } catch (StateException e) {
                guarded[0] = true;
                System.out.println("fleet.guarded: " + agent.getStateMachine().getState());

            }
            // check we're guarded
            assert guarded[0];
            assert !agent.getStateMachine().getState().equals(selfIntent);
            assert !agent.getStateMachine().getState().equals(aware);
            // satisfy the guard
            model.add(self, KNOWS, Values.iri(TEST, "Self"));
            // try the transition again ... we should skip to
            Resource transitioned = agent.getStateMachine().transition(aware);
            System.out.println("fleet.transitioned: " + transitioned);
            // check we're all good
            assert null != transitioned;
            assert selfIntent.equals(transitioned);
            assert selfIntent.equals(agent.getStateMachine().getState());
        }
    }

    // @Test
    void generateImage() throws Exception {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("image.fleet.llm.skipped: ");
            return;
        }
        System.out.println("---- image generation");
        GenericGPT gpt = new GenericGPT(OPENAI_API_KEY, LLMFactory.GPT3_5_Turbo(1000));
        System.out.println("image.fleet: " + self);

        Stopwatch stopwatch = new Stopwatch();
        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            DynamicModel memoryModel = dmf.createEmptyModel();
            ExecutiveFleet fleet = new ExecutiveFleet(self, model, model, secrets, gpt);

            fleet.intents.add(new Remodel(self, memoryModel, new ModelScriptCatalog(model)));
            fleet.deploy();
            assert !fleet.agents.isEmpty();

            I_Agentic<String> context = fleet.getContext(self);
            assert null != context;
            I_Agent agent = fleet.getAgent(self);
            assert null != agent;
            assert null != context.getConversation();

            String prompt = "Generate image of a feminine digital intelligence in pastel shades";
            context.getConversation().user(prompt);

            System.out.println("image.thread.in: @ " + stopwatch.summary());
            fleet.start();
            fleet.run();
            System.out.println("image.thread.out: @ " + stopwatch.summary());
            fleet.stop();

            System.out.println("image.done @ " + stopwatch.summary());
        }
    }

    @Test
    void testThink() throws Exception {
        if (Validate.isMissing(OPENAI_API_KEY)) {
            System.err.println("thinking.llm.skipped: ");
            return;
        }
        System.out.println("---- thinking");
        GenericGPT gpt = new GenericGPT(OPENAI_API_KEY, LLMFactory.GPT3_5_Turbo(1000));
        System.out.println("thinking.fleet: " + self);

        APISecrets secrets = new APISecrets(new EnvsAsSecrets());

        try (RepositoryConnection connection = assets.getConnection()) {
            Model model = new LiveModel(connection);
            Model thoughts = new DynamicModelFactory().createEmptyModel();
            ExecutiveFleet fleet = new ExecutiveFleet(self, model, model, secrets, gpt);
            fleet.intents.add(new Think(self, thoughts, new ModelScriptCatalog(model), gpt));
            fleet.deploy();
            I_Agent agent = fleet.getAgent(self);

            assert null != agent;
            Resource transitioned = agent.getStateMachine().transition(think);
            System.out.println("thinking.done: " + transitioned + " -> " + thoughts.size());
            RDFDump.dump(thoughts, Files.newOutputStream(new File("tested/thoughts.ttl").toPath()), RDFFormat.TURTLE);
            // check we're all good
            assert null != transitioned;
            assert think.equals(transitioned);
            assert think.equals(agent.getStateMachine().getState());
        }
    }
}