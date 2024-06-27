package systems.symbol.agent;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.*;
import systems.symbol.prompt.AgentPrompt;
import systems.symbol.secrets.SecretsException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class AgenticDecision implements I_Decide<Resource> {
private static final Logger log = LoggerFactory.getLogger(AgenticDecision.class);
I_Agentic<String> avatar;
I_LLM<String> llm;
Model model;

public AgenticDecision(I_Agentic<String> avatar, I_LLM<String> gpt, Model facts) {
this.avatar = avatar;
this.llm=gpt;
this.model = facts;
}

@Override
/**
 * Act as a manager
 * @param agent
 * @return
 */
public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
I_StateMachine<Resource> fsm = agent.getStateMachine();
log.info("llm.before: {} -> {} => {}", agent.getSelf(), fsm.getState(), avatar.getConversation().latest());
CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
try {
complete(agent, avatar);
log.info("llm.after: {} -> {} => {}", agent.getSelf(), fsm.getState(), avatar.getConversation().latest());
future.complete(fsm::getState);
} catch (Exception e) {
CompletableFuture.failedFuture(e);
} catch (APIException e) {
throw new RuntimeException(e);
}
return future;
}

public void complete(I_Agent agent, I_Agentic<String> agentic) throws APIException, IOException, SecretsException, StateException {
AgentPrompt prompt = new AgentPrompt();
// self, state and choices prompts ...
//I_Assist<String> ai = prompt.complete(new Conversation());
// user/assistant prompts ...
//I_Assist<String> chat = agentic.getConversation();
//for(int m = 0; m< chat.messages().size(); m++) {
//I_LLMessage<String> msg = chat.messages().get(m);
//if (!msg.getRole().equals(I_LLMessage.RoleType.system)) ai.add(msg);
//}
//log.info("llm.request: {}", ai);
//llm.complete(ai);
//processIntent(agent, chat ,ai);
}

private void processIntent(I_Agent agent, I_Assist<String> chat, I_Assist<String> ai) throws StateException {
if (ai.latest() instanceof IntentMessage) {
IntentMessage intent = (IntentMessage) ai.latest();
I_StateMachine<Resource> fsm = agent.getStateMachine();
if (fsm.getTransitions().contains(intent.getSelf())) {
if (intent.getSelf().equals(fsm.getState())) {
log.info("llm.nop: {}\n-> {}", fsm.getState(), intent.getContent());
chat.assistant(intent.getContent());
} else {
chat.add(intent);
fsm.transition(intent.getSelf());
log.info("llm.intent: {} => {} -> {} \n-> {}", chat.messages().size(), fsm.getState(), intent.getIntent(), intent.getContent());
}
}
} else {
String reply = ai.latest().getContent();
log.info("llm.reply: {} => {}", chat.messages().size(), reply);
chat.assistant(reply);
}
}


}
