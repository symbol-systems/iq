package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.tools.APIException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.*;
import systems.symbol.llm.gpt.CommonLLM;
import systems.symbol.platform.IQ_NS;
import systems.symbol.prompt.*;
import systems.symbol.secrets.I_Secrets;

import javax.script.Bindings;
import java.util.*;

/**
 * The `Avatar` class represents an intelligent agent capable of interacting
 * with
 * a large language model (LLM) to process and execute user intents within a
 * state
 * machine framework. This class acts as a mediator between the agent's internal
 * decision-making processes and the external prompts and responses managed by
 * the LLM.
 *
 * <ul>
 * <li>Manages agent's state machine and ensuring that state transitions
 * are performed according to the message intent.</li>
 * <li>Generating and processing prompts through the LLM, allowing the agent to
 * provide intelligent, context-aware responses.</li>
 * <li>Updating the conversation flow (`chat`) based on the LLM's output and
 * the agent's internal decision-making logic.</li>
 * <li>Heuristically determining the correct state transition based on user
 * intents, ensuring that the agent's behavior aligns with the user's
 * expectations.</li>
 * </ul>
 *
 * The intent flow in the Avatar class involves recognizing a user’s intent
 * through chat interactions, processing it using a LLM,
 * and then determining and executing the appropriate state transition within
 * the agent’s state machine.
 * The flow is designed to be modular, with clear separation of concerns between
 * intent recognition, state management, and interaction handling.
 */

public class Avatar implements I_Selfie {
private static final Logger log = LoggerFactory.getLogger(Avatar.class);
I_Agent agent;
I_Assist<String> chat;
Model facts;
I_Secrets secrets;
IRI decision = null;
int contextLength = 2048;

// Constructor initializes the Avatar instance with required dependencies
public Avatar(I_Agent agent, I_Assist<String> chat, Model facts, I_Secrets secrets) {
this.agent = agent;
this.chat = chat;
this.facts = facts;
this.secrets = secrets;
}

// Executes the agent's action given a state, LLM , and bindings
@Override
@systems.symbol.RDF(IQ_NS.IQ + "ai")
public Set<IRI> execute(IRI state, Resource llm, Bindings bindings) throws StateException {
try {
return llm(state, llm, bindings);
} catch (APIException | Exception e) {
throw new RuntimeException(e);
}
}

// Executes the agent's current/default state

public Set<IRI> execute(Bindings bindings) {
try {
return llm(getSelf(), getStateMachine().getState(), bindings);
} catch (APIException | Exception e) {
throw new RuntimeException(e);
}
}

// Handles the flow between agent and the LLM, then updating state
public Set<IRI> llm(IRI actor, Resource assistant, Bindings bindings) throws APIException, Exception {
Set<IRI> done = new HashSet<>();
I_LLM<String> llm = CommonLLM.complete(assistant, facts, contextLength, secrets);
if (llm == null) {
log.error("*** OOPS.LLM: {} @ {} *** ", actor, assistant);
return done;
}
if (!prompts(actor, assistant, llm, bindings)) {
log.warn("*** OOPS.prompt: {} @ {} *** ", actor, assistant);
return done;
}
done.add(actor);
done.add(decision);
return done;
}

public void tools(I_LLM<String> llm) {
for (Resource state : this.agent.getStateMachine().getTransitions()) {
String comment = value(state, RDFS.COMMENT);
FuncTool funct = new FuncTool(state.stringValue(), comment);
funct.requires("string", "prompt", "think, plan, prompt");
llm.tools().add(funct);
}
}

// Builds then binds the Avatar prompt from agent & state, calls LLM and updates
// the LLM
protected boolean prompts(IRI actor, Resource assistant, I_LLM<String> llm, Bindings bindings)
throws Exception, APIException {
tools(llm);
Resource state = agent.getStateMachine().getState();
bindings.put(Facades.AI, actor.getLocalName());

Optional<Literal> wrapper = Models.getPropertyLiteral(facts, assistant, RDF.VALUE);
if (wrapper.isEmpty()) {
log.warn("avatar.system.{}: {} @ {} == {}", assistant, actor, state, wrapper);
return false;
}

String prompt$ = value(agent.getSelf()) + "\n" + value(agent.getStateMachine().getState());
if (prompt$.trim().isEmpty()) {
log.warn("avatar.prompt.empty: {} @ {}", agent.getSelf(), agent.getStateMachine().getState());
return false;
}

Bindings my = Facades.rebind(agent.getSelf(), bindings);
SimplePrompt prompt = new SimplePrompt(wrapper.get().stringValue(), my);
PromptChain chain = new PromptChain(prompt);

bindings.put("prompt", prompt.bind(prompt$));
// log.info("avatar.state: {}", prompt$);
I_Assist<String> prompted = chain.complete(chat);
log.debug("avatar.completed: {} -> {}", bindings.keySet(), prompted);
I_Assist<String> answer = llm.complete(prompted);
// log.info("avatar.answer: {}", answer);
answered(agent, chat, answer);
log.debug("avatar.done: {} -> {}", actor, answer);
return true;
}

protected void links(StringBuilder s$, Iterable<IRI> found) {
found.forEach(f -> {
s$.append("[").append(f.getLocalName()).append("](").append(f.stringValue()).append("), ");
});
}

public String value(Resource state) {
if (state == null)
return null;
Optional<Literal> groundS = Models.getPropertyLiteral(facts, state, RDF.VALUE);
return groundS.orElse(Values.***REMOVED***("")).stringValue();
}

public String value(Resource state, IRI predicate) {
if (state == null)
return null;
Optional<Literal> groundS = Models.getPropertyLiteral(facts, state, predicate);
return groundS.orElse(Values.***REMOVED***("")).stringValue();
}

// Updates the chat interface with the LLM's response and handles intent
// processing
private void answered(I_Agent agent, I_Assist<String> chat, I_Assist<String> ai) throws StateException {
I_LLMessage<String> latest = ai.latest();
if (!(latest instanceof IntentMessage intent)) {
String reply = latest.getContent();
log.info("avatar.reply: {} => {}", ai.messages().size(), ai.messages());
chat.assistant(reply);
return;
}
if (latest.getRole() == I_LLMessage.RoleType.user) {
chat.messages().removeLast();
intent = new IntentMessage(intent.getIntent(), I_LLMessage.RoleType.user, latest.getContent());
log.info("avatar.intent: {}", intent);
}
chat.add(intent);
if (intent.intent() != null && !agent.getStateMachine().getState().equals(intent.intent())) {
IRI todo = next(agent, intent);
log.info("avatar.NEXT..? {} => {} @ {}", intent.getIntent(), agent.getSelf(), todo);
}
}

// Determines the appropriate state transition based on the message intent - the
// match is namespace and case agnostic
private IRI next(I_Agent agent, IntentMessage message) throws StateException {
I_StateMachine<Resource> fsm = agent.getStateMachine();
Collection<Resource> transitions = fsm.getTransitions();
String intentLocalName = message.intent().getLocalName().toLowerCase();

// match agnostic to namespace and case
for (Resource transition : transitions) {
if (transition instanceof IRI state && state.getLocalName().toLowerCase().equals(intentLocalName)) {
decision = state;
}
}
log.info("avatar.next: {} @ {} => {}", agent.getSelf(), agent.getStateMachine().getState(), decision);
return decision == null ? (IRI) agent.getStateMachine().getState() : decision;
}

// Returns the current identity of the agent
@Override
public IRI getSelf() {
return agent.getSelf();
}

// Returns the decision or intent resource
@Override
public Resource intent() throws StateException {
return decision;
}

// Retrieves the current thoughts or internal state of the agent
@Override
public Model getThoughts() {
return agent.getThoughts();
}

// Accesses the state machine of the agent
@Override
public I_StateMachine<Resource> getStateMachine() {
return agent.getStateMachine();
}

// Starts the agent's operation
@Override
public void start() throws Exception {
agent.start();
}

// Stops the agent's operation
@Override
public void stop() {
agent.stop();
}
}
