package systems.symbol.agent;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.tools.APIException;
import systems.symbol.decide.LLMDecision;
import systems.symbol.decide.ChainOfCommand;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.SearchDecision;
import systems.symbol.finder.I_ModelFinder;
import systems.symbol.finder.I_Search;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.*;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.gpt.LLMFactory;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.Facts;
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.self.SelfIntent;
import systems.symbol.string.PrettyString;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import static systems.symbol.Formats.HumanDate;
import static systems.symbol.Formats.TodayDate;
import static systems.symbol.agent.Facades.*;

public class AgentBuilder implements I_Self, I_Facade {
protected final Logger log = LoggerFactory.getLogger(getClass());
private final IRI self;
private Model ground;
private Model thoughts;
private Bindings bindings;
private final I_Secrets secrets;
private I_Intents intents;

public AgentBuilder(IRI self, Bindings bindings, I_Secrets secrets) {
this.self = self;
this.secrets = secrets;
DynamicModelFactory dmf = new DynamicModelFactory();
this.ground = dmf.createEmptyModel();
this.thoughts = dmf.createEmptyModel();
this.bindings = bindings;
this.intents = new ExecutiveIntent(self, ground);
init();
}

public AgentBuilder(IRI actor, RepositoryConnection connection, Bindings bindings, I_Secrets secrets) {
this.self = actor;
this.secrets = secrets;
DynamicModelFactory dmf = new DynamicModelFactory();
this.ground = new LiveModel(connection);
this.thoughts = dmf.createEmptyModel();
this.bindings = bindings;
this.intents = new ExecutiveIntent(self, ground, thoughts);
init();
}

public AgentBuilder scripting(I_Agent agent) throws SecretsException, StateException {
this.intents.add(new JSR233(agent, getGround(), getThoughts(), secrets, new ModelScriptCatalog(getGround())));
return this;
}

public static void meld(GraphQueryResult result, Model model) {
while (result.hasNext()) {
model.add(result.next());
}
result.close();
}

public void learn(GraphQueryResult facts) {
meld(facts, thoughts);
}

public AgentBuilder setBindings(Bindings my) {
this.bindings = my;
init();
return this;
}

public void set(String name, Object value) {
bindings.put(name, value);
}

public AgentBuilder chatty(I_Assist<String> chat) {
bindings.put("chat", chat);
bindings.put("messages", chat.messages());
bindings.put("latest", chat.latest());
log.info("builder.chatty: {} -> {}", self, chat.latest());
return this;
}

public AgentBuilder jwt(DecodedJWT jwt) {
bindings.put("jwt", jwt);
bindings.put("human", jwt.getClaim("name").asString());
return this;
}

public AgentBuilder realm(I_Realm realm) {
bindings.put("realm", realm.getSelf().stringValue());
// bindings.put("model", realm.getModel());
return this;
}

public void init() {
String iri = IdentityHelper.uuid("urn:");
bindings.put(SELF, iri);
bindings.put(NAME, PrettyString.humanize(self.getLocalName()));
bindings.put(AGENT, self.stringValue());
Date now = new Date();
bindings.put(TIME, HumanDate.format(now));
bindings.put(TODAY, "today:" + TodayDate.format(now));
bindings.put(RESULTS, new ArrayList<>());
bindings.put("size", thoughts.size());
}

public I_Agent agent() throws SecretsException, StateException {
return focus(new ExecutiveAgent(self, getGround(), getThoughts(), intents, bindings));
}

public Avatar avatar(Conversation chat) throws StateException, APIException, IOException, SecretsException {
I_Agent agent = agent();
return avatar(agent, chat);
}

public Avatar avatar(I_Agent agent, I_Assist<String> chat)
throws APIException, IOException, SecretsException, StateException {
if (agent == null || chat == null)
return null;
chatty(chat);
LLMDecision deciding = deciding(agent, chat);
Avatar avatar = new Avatar(deciding, agent, chat, getGround(), secrets);
intents.add(avatar);
log.info("builder.avatar: {}", agent.getSelf());
return avatar;
}

public I_Agent focus(I_Agent agent) {
bindings.put(FOCUS, agent.getStateMachine().getState());
Collection<Resource> intents = agent.getStateMachine().getTransitions();
bindings.put(INTENTS, Facts.toString(intents));
log.info("builder.focus: {} @ {}", agent.getSelf(), agent.getStateMachine().getState());
return agent;
}

public AgentBuilder remodel() {
intents.add(new Remodel(self, getThoughts(), new ModelScriptCatalog(getGround())));
return this;
}

public AgentBuilder search(I_ModelFinder finder) {
intents.add(new Search(self, getThoughts(), finder, getGround()));
return this;
}

public AgentBuilder sparql(RepositoryConnection connection) {
intents.add(new Select(self, connection));
intents.add(new Update(self, connection));
intents.add(new Construct(self, connection));
return this;
}

public ChainOfCommand control(I_Decide<Resource> decider) {
return new ChainOfCommand(decider);
}

public LLMDecision deciding(I_Agent agent, I_Assist<String> chat) throws SecretsException, StateException {
return deciding(agent, chat, 8000);
}

public LLMDecision deciding(I_Agent agent, I_Assist<String> chat, int contextLength)
throws SecretsException, StateException {
Optional<IRI> assistant = Models.getPropertyIRI(ground, self, Values.iri(IQ_NS.IQ + "ai"));
if (assistant.isEmpty())
return null;
I_LLM<String> llm = LLMFactory.llm(assistant.get(), ground, contextLength, secrets);
if (llm == null)
throw new StateException("agent.decisions.missing", agent.getStateMachine().getState());
Conversation decision = new Conversation();
decision.user(chat.context(2));
return new LLMDecision(llm, agent, ground, decision);
}

public SearchDecision searching(I_Search<IRI> finder, I_Assist<String> chat) {
return new SearchDecision(finder, new Agentic<>(() -> self, bindings, chat));
}

public AgentBuilder self(Conversation chat) throws APIException, IOException, StateException, SecretsException {
this.intents.add(new SelfIntent(avatar(chat), chat));
return this;
}

public AgentBuilder setThoughts(RepositoryConnection connection) {
return setThoughts(new LiveModel(connection));
}

public AgentBuilder setThoughts(Model model) {
this.thoughts = model;
bindings.put("size", thoughts.size());
return this;
}

public I_Intents getIntents() {
return intents;
}

public Model getThoughts() {
return thoughts;
}

public IRI getSelf() {
return self;
}

public Model getGround() {
return ground;
}

@Override
public Bindings getBindings() {
return bindings;
}
}
