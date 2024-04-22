package systems.symbol.fleet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.agent.AgentContext;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.I_AgentContext;
import systems.symbol.decide.ExecutiveDecision;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.Executive;
import systems.symbol.intent.JSR233;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.util.Stopwatch;

import javax.script.SimpleBindings;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ExecutiveFleet extends AgenticFleet implements I_Decide<Resource> {
    private final I_LLM<String> llm;
    private final Map<IRI, I_AgentContext<String, Resource>> contexts = new HashMap<>();
//    private final Map<IRI,Thread> workers = new HashMap<>();
    private final Map<IRI, Future<I_Delegate<Resource>>> pending = new HashMap<>();

    public ExecutiveFleet(IRI self, Model fleet, I_Secrets secrets, I_LLM<String> llm) throws StateException {
        super(self, fleet, new Executive(self, fleet), secrets);
        this.llm = llm;
        this.intents.add( new JSR233(self, fleet, secrets) );
        log.info("fleet.intents: {}", intents.getIntents());
    }

    public I_AgentContext<String, Resource> getContext(IRI agent) {
        return contexts.get(agent);
    }

    @Override
    public Future<I_Delegate<Resource>> delegate(I_Agent agent) {
        IRI actor = agent.getSelf();
        if (this.pending.containsKey(actor)) return this.pending.get(actor);

        if (awaitingPrompt(getContext(actor))) {
            log.info("decision.pending: {}", actor);
            return null;
        }

        CompletableFuture<I_Delegate<Resource>> future = new CompletableFuture<>();
//        Thread worker = new Thread(() -> {
            Stopwatch stopwatch = new Stopwatch();
            I_Delegate<Resource> delegate = delegate(future, agent, contexts.get(actor));
            log.info("decision.pending {} -> {} @ {}", actor, agent.getStateMachine().getState(), stopwatch.summary());
//            this.workers.remove(actor);
            this.pending.remove(actor);
            future.complete(delegate);

//        });
        I_LLMessage<String> latest = getContext(actor).getConversation().latest();
        log.info("decision.delegated: {} @ {} ==> {}", actor, agent.getStateMachine().getState(), latest.getContent() );
//        this.workers.put(actor, worker);
//        worker.start();
        return future;
    }

    protected I_Delegate<Resource> delegate(CompletableFuture<I_Delegate<Resource>> future, I_Agent agent, I_AgentContext<String, Resource> context) {
        this.pending.put(agent.getSelf(), future);
        return new ExecutiveDecision(llm, agent, context);
    }

    protected boolean awaitingPrompt(I_AgentContext<String,Resource> context) {
        I_LLMessage<String> latest = context.getConversation().latest();
        if (latest==null) return true;
        return !(context.getConversation().latest().getRole().equals(I_LLMessage.RoleType.user));
    }

    /**
     * Deploy a new agent instance.
     *
     * @param agent    the agent IRI
     * @return the newly created agent
     * @throws StateException if there is an issue with the deployment
     */
    public I_Agent deploy(IRI agent) throws StateException {
        I_AgentContext<String, Resource> context = new AgentContext<>(new SimpleBindings(), new ChatThread());
        contexts.put(agent, context);
        return new ExecutiveAgent(agent, fleet, intents, this, context.getBindings());
    }

    /**
     * Stops all agents in the fleet.
     *
     * @throws Exception if there is an issue stopping the agents
     */
    @Override
    public void stop() throws Exception {
        for (IRI agent : agents.keySet()) {
//            Thread thread = workers.get(agent);
//            if(thread!=null) {
//                log.warn("agent.active: {}", agent);
//                thread.interrupt();
//            }
            super.stop(agent);
        }
    }

}
