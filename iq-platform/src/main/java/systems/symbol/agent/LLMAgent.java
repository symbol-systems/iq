package systems.symbol.agent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Prompt;
import systems.symbol.llm.I_Thread;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.util.RDFHelper;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

/**
 * An agent that interacts with a Language Learning Model (LLM).
 */
public class LLMAgent implements I_Agent, I_Prompt<String> {
protected final Logger log = LoggerFactory.getLogger(getClass());
private final I_LLM<String> llm;
private final I_Agent agent;

/**
 * Constructs a new LLMAgent with the provided Language Learning Model (LLM) and base agent.
 *
 * @param llm   The Language Learning Model (LLM) to interact with.
 * @param agent The base agent for the LLMAgent.
 */
public LLMAgent(I_LLM<String> llm, I_Agent agent) {
this.llm = llm;
this.agent = agent;
}

/**
 * Checks if the LLMAgent is online.
 *
 * @return true if the LLMAgent is online, false otherwise.
 */
public boolean isOnline() {
return this.llm != null && !this.getMemo().isEmpty();
}

/**
 * Sends a message to the LLMAgent and receives a response.
 *
 * @param message The message to send to the LLMAgent.
 * @return An I_Thread containing the response from the LLMAgent.
 * @throws APIException   If an error occurs during API interaction.
 * @throws IOExceptionIf an I/O error occurs.
 * @throws StateException If an error occurs during state transition.
 */
@Override
public I_Thread<String> prompt(String message) throws APIException, IOException, StateException {
return prompt(new ChatThread(), message);
}

@Override
public I_Thread<String> prompt(I_Thread<String> thread, String prompt) throws APIException, IOException, StateException {
Literal systemPrompt = RDFHelper.label(getMemo(), getSelf());
log.info("prompt.system: {} -> {}", getSelf(), systemPrompt);
assert systemPrompt != null;
thread.system(systemPrompt.stringValue() + "\nAlways answer in JSON: { \"response\": \"{response}\", \"action\": \"{action_uri}\" }");

I_StateMachine<Resource> fsm = getStateMachine();
Resource current = fsm.getState();
Literal currentPrompt = RDFHelper.label(getMemo(), current);

StringBuilder options = new StringBuilder();
log.info("prompt.current: {}", currentPrompt);
if (currentPrompt != null) {
options.append("<current_action>");
options.append(currentPrompt);
options.append("</current_action>");
}

Collection<Resource> transitions = fsm.getTransitions();
if (!transitions.isEmpty()) {
options.append("\nOnly choose your next action from: <action_table>|allowed_action|user intention|");
transitions.forEach(t -> {
Literal label = RDFHelper.label(getMemo(), t);
if (label != null) {
options.append("\n|").append(t).append(" | ");
options.append(label.stringValue()).append(" | ");
}
});
options.append("</action_table>");
}
log.info("prompt.ai: {}", options);
thread.system(options.toString());
log.info("prompt.says: {}", prompt);
thread.user("<user_message>" + prompt + "</user_message>");
log.info("prompt.thread: {}", thread);
I_Thread<String> answer = llm.generate(thread);
Gson gson = new Gson();
String content = answer.latest().getContent();
if (content != null) {
HashMap<String, Object> reply = gson.fromJson(content, new TypeToken<HashMap<String, Object>>() {
}.getType());
String action = (String) reply.get("action");
log.info("prompt.action: {} --> {} @ {}", reply, action, fsm.getState());
if (action != null) {
IRI intent = NS.toIRI(getMemo(), getSelf(), action);
if ( intent != null) fsm.transition(intent);
log.info("prompt.intent: {} === {}", intent, fsm.getState());
}
}
return answer;
}


@Override
public Model getMemo() {
return agent.getMemo();
}

@Override
public I_StateMachine<Resource> getStateMachine() {
return agent.getStateMachine();
}


@Override
public IRI getSelf() {
return agent.getSelf();
}

@Override
public void start() throws Exception {
agent.start();
}

@Override
public void stop() throws Exception {
agent.stop();
}
}
