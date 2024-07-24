package systems.symbol.agent;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.decide.IntentDecision;
import systems.symbol.decide.ChainOfCommand;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.SearchDecision;
import systems.symbol.finder.I_ModelFinder;
import systems.symbol.finder.I_Search;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.*;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.sparql.ModelScriptCatalog;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.rdf4j.store.SelfModel;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.self.SelfIntent;

import javax.script.Bindings;
import java.util.ArrayList;

import static systems.symbol.agent.MyFacade.RESULTS;
import static systems.symbol.agent.MyFacade.STATE;

public class AgentBuilder implements I_Self {
protected final Logger log = LoggerFactory.getLogger(getClass());
private IRI self;
private Model ground;
private Model thoughts;
private Bindings bindings;
private I_Secrets secrets;
private I_Intents intents;
private final DynamicModelFactory dmf = new DynamicModelFactory();

public AgentBuilder(IRI self, Bindings bindings, I_Secrets secrets) {
this.self = self;
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

//public void setGround(Model ground) {
//this.ground = ground;
//}
//
//public void setGround(GraphQueryResult result) {
//meld(result, ground);
//}

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

//public void setThoughts(Model thoughts) {
//this.thoughts = thoughts;
//}

public void learn(GraphQueryResult facts) {
meld(facts, thoughts);
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

public AgentBuilder setBindings(Bindings my) {
this.bindings = my;
return this;
}

public void set(String name, Object value) {
bindings.put(name, value);
}

public AgentBuilder executive() throws SecretsException {
this.intents = new ExecutiveIntent(self, getGround(), getThoughts(), new JSR233(self, getGround(), getThoughts(), secrets, new ModelScriptCatalog(getGround())));
return this;
}

public ChainOfCommand decision(I_Decide<Resource> decider) {
return new ChainOfCommand(decider);
}

public IntentDecision decision(I_Assist<String> chat) {
return new IntentDecision(chat);
}

public SearchDecision decision(I_Search<IRI> finder, I_Assist<String> chat) {
return new SearchDecision(finder, new Agentic<>(()->self, bindings, chat));
}

public AgentBuilder self(Conversation chat) {
this.intents.add(new SelfIntent(self,  thoughts, chat, secrets));
return this;
}

public I_Agent build() throws SecretsException, StateException {
return new ExecutiveAgent(self, getGround(), getThoughts(), intents, null, bindings);
}

public I_Agent build(I_Decide<Resource> manager) throws SecretsException, StateException {
return new ExecutiveAgent(self, getGround(), getThoughts(), intents, manager, bindings);
}

public I_Agent build(I_Assist<String> chat) throws SecretsException, StateException {
bindings.put("messages", chat.messages());
bindings.put("latest", chat.latest());
return build(decision(chat));
}

public I_Agent build(I_Assist<String> chat, I_Decide<Resource> manager, DecodedJWT jwt) throws SecretsException, StateException {
bindings.put("messages", chat.messages());
bindings.put("latest", chat.latest());
log.info("builder.chat: {} -> {} = {}", self, chat.latest(), manager.getClass().getSimpleName());
bindings.put("jwt", jwt);
bindings.put("name", jwt.getClaim("name"));
bindings.put(RESULTS, new ArrayList<>());


ExecutiveAgent agent = new ExecutiveAgent(self, getGround(), getThoughts(), intents, manager, bindings);
intents.add(new Avatar(agent, chat, getGround(), secrets));
bindings.put(STATE, agent.getStateMachine().getState());
log.info("builder.avatar: {} -> {}", agent.getSelf(), agent.getStateMachine().getState());
return agent;
}

public AgentBuilder setGround(RepositoryConnection connection) {
this.ground = new LiveModel(connection);
return this;
}

public AgentBuilder setThoughts(RepositoryConnection connection) {
this.thoughts = new LiveModel(connection);
return this;
}

public AgentBuilder setThoughts(Model model) {
this.thoughts = model;
return this;
}

public AgentBuilder realm(I_Realm realm) {
bindings.put("realm", realm);
bindings.put("model", realm.getModel());
return this;
}

public I_Intents getIntents() {
return intents;
}
}
