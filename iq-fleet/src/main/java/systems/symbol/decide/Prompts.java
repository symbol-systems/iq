package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_AgentContext;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.I_Thread;
import systems.symbol.rdf4j.util.RDFHelper;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class Prompts {
protected static final Logger log = LoggerFactory.getLogger(Prompts.class);
public static final String INTENT = "intent"; // \""+PROMPT+"\": \"{brief_reply}\",
private static final String SYSTEM_JSON_INTENT = "\nOnly answer in valid JSON: { \"reason\": \"{explanation}\", \""+INTENT+"\": \"{user_intent_uri}\" }";
private static final String CHOOSE_INTENT = "\n|available-intent|user instruction|";

public static I_Thread<String> decision(I_Agent agent, I_AgentContext<String, Resource> context) throws IOException {
Model model = agent.getMemo();
IRI self = agent.getSelf();
I_Thread<String> thread = context.getConversation();
Bindings my = context.getBindings();
I_StateMachine<Resource> fsm = agent.getStateMachine();
// agent prompt
Set<Literal> selfPrompts = RDFHelper.values(model, self);
log.info("prompt.self: {} -> {}", self, selfPrompts);
for(Literal p: selfPrompts) {
thread.system(p.stringValue());
}
// state prompt
Resource current = fsm.getState();
Set<Literal> statePrompts = RDFHelper.values(model, current);
for(Literal p: statePrompts) {
String template = HBSRenderer.template(p.stringValue(), my);
thread.system(template);
}
log.info("prompt.state: {} -> {}", current, statePrompts);
// intents prompt
Collection<Resource> transitions = fsm.getTransitions();
log.info("prompt.transitions: {}", transitions);
if (!transitions.isEmpty()) {
StringBuilder intentsPrompt = new StringBuilder(CHOOSE_INTENT);
transitions.forEach(t -> {
Literal label = RDFHelper.label(model, t);
if (label != null) {
intentsPrompt.append("\n|").append(t).append(" | ");
intentsPrompt.append(label.stringValue()).append(" | ");
}
});
log.debug("prompt.intents: {}", intentsPrompt);
if (intentsPrompt.length()>CHOOSE_INTENT.length())
thread.system("<available-choices>"+intentsPrompt+"</available-choices>");
}
// json prompt
thread.system(SYSTEM_JSON_INTENT);
log.debug("prompt.thread: {}", thread);

return thread;
}

public static String prompt(I_Thread<String> thread, I_LLMessage.RoleType role) {
StringBuilder prompt = new StringBuilder();
for(I_LLMessage<String> message: thread.messages()) {
if (message.getRole()==role) {
prompt.append(message.getContent());
}
}
return prompt.toString();
}
}
