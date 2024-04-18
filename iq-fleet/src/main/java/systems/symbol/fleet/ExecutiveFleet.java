package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.decide.ExecutiveDelegate;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Prompt;
import systems.symbol.llm.I_Thread;
import systems.symbol.model.I_Self;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.util.Stopwatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ExecutiveFleet extends AgenticFleet implements I_Decide<Resource>, I_Prompt<String>, I_Self {
I_LLM<String> llm;
String prompt;
private final int retries;
ChatThread thread = new ChatThread();
Map<IRI,Thread> workers = new HashMap<>();

public ExecutiveFleet(IRI self, I_LLM<String> llm, Model model, EnvsAsSecrets secrets) throws StateException {
this(self, llm, model, secrets, 0);
}
public ExecutiveFleet(IRI self, I_LLM<String> llm, Model fleet, I_Secrets secrets, int retries) throws StateException {
super(self, fleet, secrets);
this.llm = llm;
this.retries = retries;
}

@Override
public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
Stopwatch stopwatch = new Stopwatch();
Thread worker = new Thread(() -> {
try {
future.complete( delegate(agent, retries) );
} catch (StateException e) {
log.error("decision.failed {} @ {}", agent.getSelf(), stopwatch.summary(), e );
future.completeExceptionally(e);
}
log.info("decision.done {} @ {}", agent.getSelf(), stopwatch.summary() );
this.workers.remove(agent.getSelf());
});
log.info("delegating: {} @ {}", agent.getSelf(), stopwatch.getStartTimestamp() );
this.workers.put(agent.getSelf(), worker);
worker.start();
return future;
}

protected I_Delegate<Resource> delegate(I_Agent agent, int attempt) throws StateException {
log.info("delegate: #{}: {} @ {}", retries-attempt, agent.getStateMachine().getState(), agent.getSelf() );
ExecutiveDelegate decision = new ExecutiveDelegate(llm, agent.getSelf(), agent.getMemo(), agent.getStateMachine());
try {
log.info("delegate.prompt: {}", prompt);
I_Thread<String> answer = decision.prompt(thread, prompt);
log.info("delegated: {} <-- {}", decision.decide(), answer.latest());
} catch (APIException | IOException e) {
log.info("retry: {} left", attempt );
if (attempt>0) return delegate(agent, attempt-1);
}
return decision;
}

@Override
public I_Thread<String> prompt(String prompt) throws APIException, IOException, StateException {
return prompt(thread,prompt);
}

@Override
public I_Thread<String> prompt(ChatThread history, String prompt) throws APIException, IOException, StateException {
thread.user(prompt);
return llm.generate(thread);
}
}
