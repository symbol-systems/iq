package systems.symbol.decide;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_AgentContext;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.I_Thread;
import systems.symbol.string.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static systems.symbol.agent.MyFacade.PROMPT;

/*
 * An LLM decision maker that uses a Language Model (LLM) to interpret an actor's intentions on behalf of an agent .
 */
public class ExecutiveDecision implements I_Delegate<Resource> {
protected final Logger log = LoggerFactory.getLogger(getClass());

private final I_LLM<String> llm;
private final Gson gson = new Gson();
private List<Map<String, Object>> history = new ArrayList<>();

I_Agent agent;
I_AgentContext<String,Resource> context;
/*
 * Constructor for an ExecutiveDecision strategy
 * @param llm The LLM used for decision-making.
 * @param agent The agent requesting a decision.
 * @param agent The FSM associated with the decision.
 */
public ExecutiveDecision(I_LLM<String> llm, I_Agent agent, I_AgentContext<String,Resource> context) {
this.llm = llm;
this.agent = agent;
this.context = context;
}

/*
 * Make a decision based on the current prompt and historic context.
 * @param history An existing ChatThread for historic context.
 * @param prompt The prompt message to base the decision on.
 * @throws APIException If an API-related error occurs.
 * @throws IOException If an IO-related error occurs.
 * @throws StateException If an error occurs with the state machine.
 */
@Override
public Resource decide() throws StateException {
try {
I_Thread<String> situation = Prompts.decision(agent, context);
if (situation.latest()==null) {
throw new StateException("not ready", agent.getStateMachine().getState());
}
String prompt = Prompts.prompt(situation, I_LLMessage.RoleType.user);
log.info("executive.prompt: {}", prompt);
context.getBindings().put(PROMPT, prompt);
I_Thread<String> response = llm.complete(situation);

String content = response.latest().getContent();
log.info("executive.response: {}", content);
if ( Validate.isMissing(content) ) return null;

Map<String, Object> reply = gson.fromJson( content, new TypeToken<HashMap<String, Object>>() {}.getType() );
history.add(reply);
String intent = (String) reply.get(Prompts.INTENT);
log.info("executive.intent: {} --> {}", reply, intent);
return Values.iri(intent);
} catch (IOException | APIException e) {
throw new StateException(e.getMessage(), agent.getStateMachine().getState(), e);
}
}

public List<Map<String, Object>> getHistory() {
return history;
}

}
