package systems.symbol.platform;

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
import systems.symbol.intent.*;
import systems.symbol.llm.DefaultLLConfig;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMConfig;
import systems.symbol.llm.gpt.GenericGPT;
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

    public AvatarBuilder executive() {
//        log.info("executive: {}", getSelf());
//        Iterable<Statement> statements = getGround().getStatements(getSelf(), null, null);
//        statements.forEach( (s) -> {
//            log.info("\t{} -> {}", s.getPredicate(), s.getObject());
//        });
        this.intents = new ExecutiveIntent(self, getGround(), new JSR233(self, getThoughts(), getSecrets()));
        return this;
    }

    public AvatarBuilder remodel() {
        intents.add(new Remodel(self, getThoughts(), new ModelScriptCatalog(getGround()) ));
        return this;
    }

    public AvatarBuilder search(I_FactFinder finder) {
        intents.add(new Search(self, getThoughts(), finder, getGround() ));
        return this;
    }

    public AvatarBuilder sparql(RepositoryConnection connection) {
        intents.add(new Select(self, connection));
        intents.add(new Update(self, connection));
        intents.add(new Construct(self, connection));
        return this;
    }

    public I_LLMConfig configure() {
        DefaultLLConfig config = new DefaultLLConfig(contextLength);
        try {
            Poke.poke(getSelf(), getGround(), config);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    public I_LLM<String> llm() throws SecretsException {
        I_LLMConfig config = configure();
        String secretName = config.getSecretName();
        log.info("llm.secrets: {} @ {} -> {} -> {}", self, secretName, config.getName(), config.getURL());
        if (secretName==null||secretName.isEmpty()) throw new SecretsException("secret.missing: "+config.getName());
        String token = secrets.getSecret(secretName);
        if (token==null) throw new SecretsException("missing: "+secretName);
        return new GenericGPT(token, config);
    }

    public Bindings getBindings() {
        return bindings;
    }

    public void setBindings(Bindings my) {
        this.bindings = my;
    }

    public I_Secrets getSecrets() {
        return secrets;
    }

    public void setSecrets(I_Secrets secrets) {
        this.secrets = secrets;
    }

    public void set(String name, Object value) {
        bindings.put(name, value);
    }

    public Avatar build() throws SecretsException {
        return new Avatar(getSelf(), getGround(), getThoughts(), llm(), getBindings(), getSecrets());
    }

}
