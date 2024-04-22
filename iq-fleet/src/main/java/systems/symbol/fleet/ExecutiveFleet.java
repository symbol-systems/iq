package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.decide.ExecutiveDelegate;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Executive;
import systems.symbol.intent.JSR233;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Prompt;
import systems.symbol.llm.I_Thread;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ExecutiveFleet extends AgenticFleet implements I_Decide<Resource>, I_Prompt<String>, I_Self {
private final Bindings bindings;
private int retries = 0;
private final I_LLM<String> llm;
private final I_Thread<String> manager = new ChatThread();
private final Map<IRI, Bindings> scratch = new HashMap<>();
private final Map<IRI,Thread> workers = new HashMap<>();
private final Map<IRI, Future<I_Delegate<Resource>>> futures = new HashMap<>();

public ExecutiveFleet(IRI self, Model fleet, I_Secrets secrets, I_LLM<String> llm, Bindings bindings) {
super(self, fleet, new Executive(self, fleet), secrets);
this.llm = llm;
this.bindings = bindings;
this.intents.add( new JSR233(self, fleet, secrets) );
log.info("fleet.intents: {} -> {}", intents.getIntents(), bindings.values());
}

public void setRetries(int retries) {
this.retries = retries;
}

public I_Thread<String> newChat(I_Agent agent) {
I_Thread<String> chat = new ChatThread(manager);
log.info("decision.chat {} @ {}", agent.getSelf(), chat.messages() );
return chat;
}

@Override
public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
IRI actor = agent.getSelf();
if (this.futures.containsKey(actor)) return this.futures.get(actor);

CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
this.futures.put(actor, future);
Stopwatch stopwatch = new Stopwatch();

log.info("decision.delegate {} @ {}", actor, scratch.values() );
Thread worker = new Thread(() -> {
try {
// delegate our decision
future.complete( delegate(agent, retries) );
} catch (StateException e) {
log.error("decision.failed {} @ {}", actor, stopwatch.summary(), e );
future.completeExceptionally(e);
}
log.info("decision.done {} @ {}", actor, stopwatch.summary() );
this.workers.remove(actor);
this.futures.remove(actor);
});

log.info("delegating: {} @ {}", actor, stopwatch.getStartTimestamp() );
this.workers.put(actor, worker);
worker.start();
return future;
}

protected I_Delegate<Resource> delegate(I_Agent agent, int attempt) throws StateException {
log.info("delegate: #{}: {} @ {}", retries-attempt, agent.getStateMachine().getState(), agent.getSelf() );

ExecutiveDelegate delegation = new ExecutiveDelegate(llm, agent.getSelf(), agent.getMemo(), agent.getStateMachine());
try {
I_Thread<String> chat = newChat(agent);
Bindings my = getScratchPad(agent.getSelf());
log.info("delegate.history: {} -> {}", my, chat.messages());
I_Thread<String> answer = delegation.decide(chat, my);

log.info("delegated: {} <-- {}", delegation.decide(), answer.latest());
} catch (APIException | IOException e) {
log.info("retry: {} left", attempt );
if (attempt>0) return delegate(agent, attempt-1);
}
return delegation;
}

@Override
public I_Thread<String> prompt(String prompt) throws APIException, IOException, StateException {
return prompt(manager,prompt);
}

@Override
public I_Thread<String> prompt(I_Thread<String> history, String prompt) throws APIException, IOException, StateException {
manager.user(prompt);
return manager;
}

/**
 * Creates a new agent instance.
 *
 * @param selfthe self IRI representing the agent
 * @return the newly created agent
 * @throws StateException if there is an issue with the state machine
 */
public I_Agent newAgent(IRI self) throws StateException {
return new ExecutiveAgent(self, fleet, intents, this, getScratchPad(self));
}

/**
 * Return the working context of an Agent
 * @param agent The IRI of a fleet agent
 * @return scratch-pad bindings
 */
public Bindings getScratchPad(IRI agent) {
Bindings bindings = scratch.get(agent);
if (bindings!=null) return bindings;
bindings = new SimpleBindings(this.bindings);
scratch.put(agent, bindings);
return bindings;
}

/**
 * Stops all agents in the fleet.
 *
 * @throws Exception if there is an issue stopping the agents
 */
@Override
public void stop() throws Exception {
for (IRI agent : agents.keySet()) {
Thread thread = workers.get(agent);
if(thread!=null) {
log.warn("agent.active: {}", agent);
thread.interrupt();
}
super.stop(agent);
}
}

}
