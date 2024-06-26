package systems.symbol.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.MyFacade;
import systems.symbol.agent.tools.APIException;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.*;
import systems.symbol.platform.AvatarBuilder;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Avatar implements I_Self, I_Decide<Resource> , I_Intent {
    private static final Logger log = LoggerFactory.getLogger(Avatar.class);
    private final I_LLM<String> llm;
    Handlebars hbs = new Handlebars();
    protected IRI self;
    protected Model ground;
    protected Model thoughts;
    //    protected I_Assist<String> cogs = new Conversation();
    protected Bindings my;
    protected I_Secrets secrets;
    protected String md_blocks_regex = "```(?:([a-zA-Z0-9_]+))?\\n([\\s\\S]*?)\\n```";

    public Avatar(IRI self, Model ground, Model thoughts, I_LLM<String> llm, Bindings my, I_Secrets secrets) {
        this.self = self;
        this.ground = ground;
        this.thoughts = thoughts;
        this.llm = llm;
        this.my = my;
        this.secrets = secrets;
        this.hbs.registerDecorator("self", new Decorator() {
            @Override
            public void apply(Template template, Options options) throws IOException {

            }
        });
        this.hbs.registerHelper("self", new Helper<Object>() {
            @Override
            public Object apply(Object o, Options options) throws IOException {
                return null;
            }
        });
    }

    @Override
    public IRI getSelf() {
        return self;
    }

    protected void complete(I_Assist<String> chat, I_Agent agent, Bindings bindings, CompletableFuture<I_Delegate<Resource>> future) throws IOException, APIException, SecretsException, StateException {
        IRI complete = complete(chat, agent, bindings);
        future.complete(() -> complete);
    }

    public IRI complete(I_Assist<String> chat, I_Agent agent, Bindings bindings) throws APIException, IOException, SecretsException, StateException {
        Conversation ai = new Conversation();
        I_StateMachine<Resource> fsm = agent.getStateMachine();
        // system prompt
        prompt(ai, agent.getSelf(), fsm.getState(), bindings);
        // intent prompt
        prompts(ai, fsm.getTransitions(), bindings);
        // user prompt
        I_LLMessage<String> latest = chat.latest();
        if (latest != null)
            ai.user(latest.getContent());

        log.info("avatar.complete: {} => {}", fsm.getState(), latest);
        llm.complete(ai);
        String reply = ai.latest().getContent();

        // JSON response
        if (reply.startsWith("{") && reply.endsWith("}")) {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleBindings decision = new SimpleBindings(objectMapper.readValue(reply, java.util.Map.class));

            String content = String.valueOf(decision.get("content"));
            IRI intent = Values.iri(String.valueOf(decision.get("intent")));
            log.info("avatar.intent: {} => {}", content, intent);
            chat.assistant(content);
            Resource transitioned = fsm.transition(intent);
            log.info("avatar.trans: {}", transitioned);
        } else {
            // markdown blocks
            Pattern pattern = Pattern.compile(md_blocks_regex);
            Matcher matcher = pattern.matcher(reply);
            if (matcher.matches()) {
                // TODO: add with provenance
                // Provenance.generated();
                log.info("avatar.match: {} => {}", latest.getContent(), matcher);
            } else {
                log.info("avatar.matched: {}", latest.getContent());
            }
        }
        chat.assistant(reply);
        return (IRI) fsm.getState();
    }

    protected I_Assist<String> prompt(I_Assist<String> chat, IRI actor, Resource state, Bindings bindings) throws SecretsException, IOException, APIException {
        bindings.putAll(this.my);
        Bindings my = MyFacade.bind(actor, state, thoughts, bindings, secrets);

        // prompt building
        StringBuilder prompt$ = new StringBuilder();

        // the grounding
        Optional<Literal> groundA = Models.getPropertyLiteral(this.ground, actor, RDF.VALUE);
        groundA.ifPresent(literal -> prompt$.append(literal.stringValue()));

        Optional<Literal> thoughtA = Models.getPropertyLiteral(this.thoughts, actor, RDF.VALUE);
        thoughtA.ifPresent(literal -> prompt$.append(literal.stringValue()));

        // the stateful
        if (state!=null) {
            Optional<Literal> groundS = Models.getPropertyLiteral(this.ground, state, RDF.VALUE);
            groundS.ifPresent(literal -> prompt$.append(literal.stringValue()));

            Optional<Literal> thoughtS = Models.getPropertyLiteral(this.thoughts, state, RDF.VALUE);
            thoughtS.ifPresent(literal -> prompt$.append(literal.stringValue()));
        }

        // interpolate system prompt
        String prompt = hbs.compileInline(prompt$.toString()).apply(my);
        log.info("prompted: {} -> {} --> {}", actor, state, prompt);
        assert !prompt.isEmpty();
        chat.messages().add(new TextMessage(I_LLMessage.RoleType.system, prompt));
        return chat;
    }

    public I_Assist<String> prompts(I_Assist<String> chat, Collection<Resource> choices, Bindings bindings) throws IOException {
        StringBuilder intent$ = new StringBuilder();
        choices.forEach( c -> {
            Iterable<Statement> options = ground.getStatements(c, RDFS.LABEL, null);
            log.info("prompt.choice: {} -> {}", c, options.iterator().hasNext());
            options.forEach( o ->{
                intent$.append("\n").append(o.getSubject().stringValue()).append(" = ").append(o.getObject().stringValue());
            });
        });
        String intents = hbs.compileInline(intent$.toString()).apply(bindings);
        if (!intents.isEmpty()) chat.messages().add(new TextMessage(I_LLMessage.RoleType.assistant, intents));
        return chat;
    }

//    private void prompt(Iterable<Statement> groundsA, StringBuilder prompt$) {
//        groundsA.forEach(statement -> {
//            prompt$.append(statement.getObject().stringValue());
//        });
//    }

    public Model getThoughts() {
        return this.thoughts;
    }

    public Model getGround() {
        return ground;
    }

    public Resource next(I_Agent agent) throws StateException, ExecutionException, InterruptedException {
        Future<I_Delegate<Resource>> delegate = delegate(agent);
        I_Delegate<Resource> decided = delegate.get();
        Resource next = decided.decide();
        System.out.println("avatar.agent.operate: " + next + " @ " + agent.getSelf());
        return agent.getStateMachine().transition(next);
    }

    public Future<I_Delegate<Resource>> delegate(I_Agent agent) throws StateException {
        I_StateMachine<Resource> fsm = agent.getStateMachine();
        log.info("delegate.agent: {} -> {}", agent.getSelf(), fsm.getState());
        CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
        try {
            log.info("delegate.fsm: {} -> {} -> {}", agent.getSelf(), fsm.getState(), fsm.getTransitions());
            // Perform prompts and complete llm action
            complete(new Conversation(), agent, new SimpleBindings(), future);
        } catch (APIException | SecretsException | IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    @systems.symbol.RDF(IQ_NS.IQ + "a")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
        Set<IRI> done = new HashSet<>();
        try {
            I_Assist<String> chat = prompt(new Conversation(), actor, state, bindings);
            llm.complete(chat);
            done.add(actor);
        } catch (SecretsException e) {
            throw new StateException(actor.stringValue()+"#trust", state);
        } catch (IOException e) {
            throw new StateException(e.getMessage(), state);
        } catch (APIException e) {
            throw new StateException(actor.stringValue()+"#api", state);
        }
        return done;
    }

    public void memorize(RepositoryConnection connection) {
        connection.begin();
        connection.add(getThoughts(), getSelf());
        connection.commit();
    }

}
