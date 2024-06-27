package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.finder.I_FactFinder;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.*;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import javax.script.Bindings;

public class AvatarBuilder implements I_Self {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private IRI self;
    private Model ground;
    private Model thoughts;
    private int contextLength = 2000;
    private Bindings bindings;
    private I_Secrets secrets;
    private I_Intents intents;
    private final DynamicModelFactory dmf = new DynamicModelFactory();
//    private Conversation chat;

    public AvatarBuilder(IRI self, int contextLength, Bindings bindings, I_Secrets secrets) {
        this.self = self;
        this.contextLength = contextLength;
        this.secrets = secrets;
        this.ground = dmf.createEmptyModel();
        this.thoughts = dmf.createEmptyModel();
        this.bindings = bindings;
    }

    public IRI getSelf() {
        return self;
    }

    public Model getGround() {
        return ground;
    }

    public void setGround(Model ground) {
        this.ground = ground;
    }

    public void setGround(GraphQueryResult result) {
        meld(result, ground);
    }

    public static void meld(GraphQueryResult result, Model model) {
        while (result.hasNext()) {
            model.add(result.next());
        }
        result.close();
    }

    public void setGround(RepositoryResult<Statement> result) {
        while (result.hasNext()) {
            ground.add(result.next());
        }
        result.close();
    }

    public Model getThoughts() {
        return thoughts;
    }

    public void setThoughts(Model thoughts) {
        this.thoughts = thoughts;
    }

    public void learn(GraphQueryResult facts) {
        meld(facts, thoughts);
    }

    public AvatarBuilder remodel() {
        intents.add(new Remodel(self, getThoughts(), new ModelScriptCatalog(getGround())));
        return this;
    }

//    public AvatarBuilder avatar() {
//        intents.add(new Avatar(self, getGround(), getThoughts(), contextLength, getBindings(), getSecrets() ));
//        return this;
//    }

    public AvatarBuilder search(I_FactFinder finder) {
        intents.add(new Search(self, getThoughts(), finder, getGround()));
        return this;
    }

    public AvatarBuilder sparql(RepositoryConnection connection) {
        intents.add(new Select(self, connection));
        intents.add(new Update(self, connection));
        intents.add(new Construct(self, connection));
        return this;
    }

//    public Bindings getBindings() {
//        return bindings;
//    }

    public AvatarBuilder setBindings(Bindings my) {
        this.bindings = my;
        return this;
    }

//    public I_Secrets getSecrets() {
//        return secrets;
//    }

    public AvatarBuilder setSecrets(I_Secrets secrets) {
        this.secrets = secrets;
        return this;
    }

    public void set(String name, Object value) {
        bindings.put(name, value);
    }

    public AvatarBuilder executive() throws SecretsException {
        this.intents = new ExecutiveIntent(self, getGround(), getThoughts(), new JSR233(self, getGround(), getThoughts(), secrets, new ModelScriptCatalog(getGround())));
        return this;
    }

    public I_Agent build() throws SecretsException, StateException {
        return new ExecutiveAgent(self, getGround(), getThoughts(), intents, null, bindings);
    }

    public I_Agent build(I_Assist<String> chat) throws SecretsException, StateException {
        log.info("builder.thoughts: {} -> {}", self, chat.latest());
        Agentic<String, Object> agentic = new Agentic<>(()->self, bindings, chat);
//        AgenticDecision manager = new AgenticDecision(agentic, gpt, getGround());

        ExecutiveAgent agent = new ExecutiveAgent(self, getGround(), getThoughts(), intents, null, agentic.getBindings());
        intents.add(new Avatar(agent, chat, getGround(), secrets));

        log.info("builder.self: {} -> {}", agent.getSelf(), agent.getStateMachine().getState());
        return agent;
    }

    public AvatarBuilder setGround(RepositoryConnection connection) {
        this.ground = new LiveModel(connection);
        return this;
    }

    public AvatarBuilder setThoughts(RepositoryConnection connection) {
        this.thoughts = new LiveModel(connection);
        return this;
    }
}
