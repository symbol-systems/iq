package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.tools.APIException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.IntentMessage;
import systems.symbol.llm.tools.Tool;
import systems.symbol.llm.tools.Tool.FunctionBuilder;
import systems.symbol.string.PrettyString;

import java.io.IOException;
import java.util.Optional;

/*
 * An LLM decision maker that uses a Language Model (LLM) to interpret 
 * an actor's intentions on behalf of an agent .
 */
public class LLMDecision implements I_Delegate<Resource> {
protected final Logger log = LoggerFactory.getLogger(getClass());

private final I_LLM<String> llm;
I_Assist<String> prompts;
Model ground;
I_Agent agent;

/*
 * Constructor for an ExecutiveDecision strategy
 * 
 * @param llm The LLM used for decision-making.
 * 
 * @param agent The agent requesting a decision.
 * 
 * @param agent The FSM associated with the decision.
 */
public LLMDecision(I_LLM<String> llm, I_Agent agent, Model ground,
I_Assist<String> prompts) {
this.llm = llm;
this.agent = agent;
this.prompts = prompts;
this.ground = ground;
}

/*
 * Make a decision based on the current prompt and historic context.
 * 
 * @param history An existing ChatThread for historic context.
 * 
 * @param prompt The prompt message to base the decision on.
 * 
 * @throws APIException If an API-related error occurs.
 * 
 * @throws IOException If an IO-related error occurs.
 * 
 * @throws StateException If an error occurs with the state machine.
 */

public Resource decides() throws StateException, IOException, APIException {
I_StateMachine<Resource> fsm = agent.getStateMachine();
Resource state = fsm.getState();
if (fsm.getTransitions().isEmpty())
return state;
if (fsm.getTransitions().size() == 1)
return fsm.getTransitions().iterator().next();

log.info("llm.decide.state: {}", state);
for (Resource iri : fsm.getTransitions()) {
String name = ((IRI) iri).getLocalName();
Optional<Literal> comment = Models.getPropertyLiteral(ground, iri, RDFS.COMMENT);
String description = comment.isPresent() ? comment.get().stringValue()
: PrettyString.humanize(name);

FunctionBuilder fn = Tool.defineFunction(name, description);
fn.addStringParam("content", "reply", false);
Tool tool = fn.build();
// log.info("llm.decide.tool: {} -> {}", iri, tool);
llm.tools().add(tool);
}

log.info("llm.decide.prompt: {}", prompts);
I_LLMessage<String> latest = llm.complete(prompts).latest();
if (latest == null) {
log.info("llm.decide.nothing");
return state;
}
if (!(latest instanceof IntentMessage)) {
log.info("llm.decide.text: {} ", latest.getContent());
return state;
}
IntentMessage intent = (IntentMessage) latest;
if (intent.getIntent() == null || intent.getIntent().isEmpty())
return state;
if (intent.getIntent().indexOf(":") > 0) {
log.info("llm.decide.intent: {}", intent.getIntent());
return Values.iri(intent.getIntent());
}

// heuristic
for (Resource t : fsm.getTransitions()) {
if (t.isIRI()) {
IRI iri = (IRI) t;
if (iri.getLocalName().equalsIgnoreCase(intent.getIntent())) {
return iri;
}
}
}
log.info("llm.decide.state: {} -> {}", intent, state);
return state;
}

@Override
public Resource intent() throws StateException {
try {
return decides();
} catch (StateException e) {
log.error("llm.decide.state: {}", e);
} catch (IOException e) {
log.error("llm.decide.io: {}", e);
} catch (APIException e) {
log.error("llm.decide.api: {}", e);
}
return null;
}
}
