package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.Agentic;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_Agentic;
import systems.symbol.decide.FutureDecision;
import systems.symbol.decide.LLMDecision;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.intent.JSR233;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_LLM;
import systems.symbol.secrets.I_Secrets;

import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ExecutiveFleet extends AgenticFleet implements I_Decide<Resource>, Runnable {
    private final I_LLM<String> llm;
    private final Map<IRI, I_Agentic<String>> contexts = new HashMap<>();
    private final Map<IRI, CompletableFuture<I_Delegate<Resource>>> pending = new HashMap<>();
    private boolean isRunning = false;
    private long sleepTime = 100;

    public ExecutiveFleet(IRI self, Model fleet, I_Secrets secrets, I_LLM<String> llm) throws StateException {
        super(self, fleet, new ExecutiveIntent(self, fleet), secrets);
        this.llm = llm;
        this.intents.add( new JSR233(self, fleet, secrets) );
        log.info("fleet.intents: {}", intents.getIntents());
    }

    public I_Agentic<String> getContext(IRI agent) {
        return contexts.get(agent);
    }

    @Override
    public Future<I_Delegate<Resource>> delegate(I_Agent agent) throws StateException {
        IRI actor = agent.getSelf();
        if (this.pending.containsKey(actor)) return this.pending.get(actor);
        CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
        this.pending.put( actor, future);
        new Thread(() -> {
            try {
                process(actor);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        log.info("decision.delegated: {} @ {}", actor, agent.getStateMachine().getState());
        return future;
    }

    @Override
    public void run() {
        log.info("running: {}",isRunning);
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        while(isRunning && !pending.isEmpty()) {
            try {
                log.info("decision.loop: {}",pending.keySet());
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void process(IRI actor) throws ExecutionException, InterruptedException {
        if (!isRunning) return; // forceful
        I_Agent agent = getAgent(actor);
        CompletableFuture<I_Delegate<Resource>> decider = pending.get(actor);
        log.info("decision.pending {} @ {}", actor, agent.getStateMachine().getState());
        if (decider==null) return; // quietly

        try {
            Resource decision = decider.get().decide();
            if (decision!=null) {
                pending.remove(actor);
                decider.complete(() -> decision);
            }
        } catch (StateException e) {
            log.error("decision.failed: {} -> {}", actor, e.getMessage(), e);
        }

    }

    protected Future<I_Delegate<Resource>> delegate(I_Agent agent, I_Agentic<String> context) throws StateException {
        LLMDecision decision = new LLMDecision(llm, agent, context);

        IRI decided = decision.decide();
        FutureDecision<Resource> futureDecision = new FutureDecision<>() {
        };
        return futureDecision;
    }

    /**
     * Deploy a new contextual Agent who delegates to this.
     *
     * @param actor    the agent IRI
     * @return the newly created agent
     * @throws StateException if there is an issue with the deployment
     */
    public I_Agent deploy(IRI actor) throws StateException {
        if (this.agents.containsKey(actor)) return agents.get(actor);
        I_Agentic<String> context = new Agentic<>(new SimpleBindings(), new Conversation());
        contexts.put(actor, context);
        ExecutiveAgent agent = new ExecutiveAgent(actor, fleet, intents, this, context.getBindings());
        agents.put(actor,agent);
        return agent;
    }

    /**
     * starts the fleet of agents
     * @throws Exception throws an error is something went wrong
     */
    public void start() throws Exception {
        isRunning = true;
    }
    /**
     * Stops all agents in the fleet.
     */
    @Override
    public void stop() {
        log.info("stop.gratefully: {}", pending.keySet());
        long start = System.currentTimeMillis();
        while (!pending.isEmpty() && (System.currentTimeMillis()-start<5000)) {
            try {
                Thread.sleep(1000);
                isRunning = false;
                super.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
