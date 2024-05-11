package systems.symbol.llm;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_AgentContext;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.rdf4j.util.RDFHelper;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static systems.symbol.agent.MyFacade.PROMPT;

public class Prompts {
protected static final Logger log = LoggerFactory.getLogger(Prompts.class);
public static final String INTENT = "intent";
public static String CONTENT = "content";
private static final String SYSTEM_JSON_INTENT = "\nOnly answer in valid JSON: { \""+CONTENT+"\": \"{explanation}\", \""+INTENT+"\": \"{user_intent_uri}\" }";
private static final String CHOOSE_INTENT = "\n|available-intent|user instruction|";
protected static int max_tokens = 1024;

public static I_Chat<String> decision(I_Agent agent, I_AgentContext<String, Resource> context, int tokens) throws IOException {
IRI actor = agent.getSelf();
Model model = agent.getMemo();
Bindings my = context.getBindings();
I_StateMachine<Resource> fsm = agent.getStateMachine();
Resource current = fsm.getState();
// actor/state prompt
I_Chat<String> thread = prompt(actor, current, model, my);
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
// json system-prompt
thread.system(SYSTEM_JSON_INTENT);
log.debug("prompt.thread: {}", thread);
// copy non-system
for(I_LLMessage<String> message: context.getConversation().messages()) {
if (message.getRole()!=I_LLMessage.RoleType.system) {
thread.add(message);
}
}
// copy user-prompt
String prompt = Prompts.user(thread);
log.info("prompt.user: {}", prompt);
my.put(PROMPT, prompt);
return thread;
}

private static I_Chat<String> prompt(IRI actor, Resource current, Model model, Bindings my) throws IOException {
Conversation thread = new Conversation();
Set<Literal> selfPrompts = RDFHelper.values(model, actor);
log.info("prompt.actor: {} -> {}", actor, selfPrompts);
for(Literal p: selfPrompts) {
String template = HBSRenderer.template(p.stringValue(), my);
thread.system(template);
}
// state prompt
Set<Literal> statePrompts = RDFHelper.values(model, current);
for(Literal p: statePrompts) {
String template = HBSRenderer.template(p.stringValue(), my);
thread.system(template);
}
log.info("prompt.state: {} -> {}", current, statePrompts);
return thread;
}

public static String user(I_Chat<String> thread) {
return prompt(thread, I_LLMessage.RoleType.user);
}


public static String prompt(I_Chat<String> thread, I_LLMessage.RoleType role) {
StringBuilder prompt = new StringBuilder();
for(I_LLMessage<String> message: thread.messages()) {
if (message.getRole()==role) {
if (prompt.length()>0) prompt.append("\n");
prompt.append(message.getContent());
}
}
return prompt.toString();
}

public static I_Chat<String> think(IRI actor, Resource state, String intent, Model model, Bindings my, RDFFormat format) throws IOException {
I_Chat<String> thread = prompt(actor, state, model, my);
thread.system("Reply using valid "+format.getName()+". Use only these namespaces - remember to declare 'my:' prefix");
Map<String, String> namespaces = RDFPrefixer.simple();
namespaces.put("my", intent);
thread.system(RDFPrefixer.toPrefix(namespaces).toString());
return thread;
}
}
