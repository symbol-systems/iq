package systems.symbol.fleet;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.agent.I_Agent;
import systems.symbol.decide.I_Decision;
import systems.symbol.decide.I_Delegate;
import systems.symbol.decide.SimpleDecision;
import systems.symbol.fsm.StateException;
import systems.symbol.secrets.I_Secrets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class FleetCommand extends AgenticFleet implements I_Delegate<Resource> {
    private final Map<I_Agent, CompletableFuture<I_Decision<Resource>>> pending = new HashMap<>();

    public FleetCommand(Model fleet, I_Secrets secrets) throws StateException {
        super(fleet, secrets);
    }

    @Override
    public Future<I_Decision<Resource>> delegate(I_Agent agent) {
        CompletableFuture<I_Decision<Resource>> future = new CompletableFuture<>();
        log.info("pending: {}", agent.getSelf() );
        this.pending.put(agent, future);
        return future;
    }

    public Set<I_Agent> getPending() {
        return pending.keySet();
    }

    public void decide(@NotNull I_Agent agent, @NotNull Resource state) {
        CompletableFuture<I_Decision<Resource>> future = pending.get(agent);
        if (future==null) return;
        future.complete(new SimpleDecision<>(agent.getStateMachine(), state));
        pending.remove(agent);
    }
}
