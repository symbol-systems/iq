package systems.symbol.decide;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.LLMAgent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Prompt;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Thread;
import systems.symbol.render.HBSRenderer;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashMap;

/*
 * An LLM decision maker that uses a Language Model (LLM) to interpret an actor's intentions on behalf of an agent .
 */
public class LLMDelegate extends SimpleDelegate<Resource> implements I_Prompt<String> {
protected final Logger log = LoggerFactory.getLogger(getClass());
private final I_LLM<String> llm;
private final I_Agent agent;
private final Gson gson = new Gson();
private final Bindings my;

/*
 * Constructor for LLMDecider.
 * @param llm The LLM used for decision-making.
 * @param agent The agent associated with the decision-making process.
 */
public LLMDelegate(I_LLM<String> llm, I_Agent agent, Bindings my) {
super(agent.getStateMachine());
this.agent = new LLMAgent(llm, agent);
this.llm = llm;
this.my = my;
}

/*
 * make a decision based on the provided prompt.
 * @param prompt The prompt message used to base the decision on.
 * @return The decision chai, represented as an IRI or NULL if the LLM response was empty.
 * @throws APIException If an API-related error occurs.
 * @throws IOException If an IO-related error occurs.
 * @throws StateException If an error occurs with the state machine.
 */
public I_Thread<String> prompt(String prompt) throws APIException, IOException, StateException {
return prompt(new ChatThread(), prompt);
}

/*
 * make a decision based on the current prompt and historic context.
 * @param history An existing ChatThread for historic context.
 * @param prompt The prompt message to base the decision on.
 * @throws APIException If an API-related error occurs.
 * @throws IOException If an IO-related error occurs.
 * @throws StateException If an error occurs with the state machine.
 */
public I_Thread<String> prompt(I_Thread<String> history, String prompt) throws APIException, IOException {
I_StateMachine<Resource> fsm = this.agent.getStateMachine();
assert null != fsm;
log.info("decide.state: " + agent.getSelf() + " -> " + fsm.getState());

I_Thread<String> prompted = Prompts.decision(history, agent.getMemo(), agent.getSelf(), fsm, my);
prompted.user(HBSRenderer.template(prompt, my));

I_Thread<String> answer = llm.generate(prompted);
log.info("decide.answer: " + answer.latest());
String content = answer.latest().getContent();
if (Validate.isMissing(content)) return null;

HashMap<String, Object> reply = gson.fromJson(content, new TypeToken<HashMap<String, Object>>() {}.getType());
String intent = (String) reply.get("intent");
log.info("decide.intent: {} --> {}", reply, intent);
choice(Values.iri(intent));
return answer;
}
}
