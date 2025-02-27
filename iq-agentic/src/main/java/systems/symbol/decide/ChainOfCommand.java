package systems.symbol.decide;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.rdf4j.IRIs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChainOfCommand implements I_Decide<Resource> {
    private static final Logger log = LoggerFactory.getLogger(ChainOfCommand.class);
    private final List<I_Decide<Resource>> delegates = new ArrayList<>();
    IRIs seen = new IRIs();

    public ChainOfCommand() {
    }

    public ChainOfCommand(I_Decide<Resource> delegate) {
        this.delegates.add(delegate);
    }

    public void add(I_Decide<Resource> delegate) {
        delegates.add(delegate);
    }

    @Override
    public Future<I_Delegate<Resource>> delegate(I_Agent agent) throws StateException {
        CompletableFuture<I_Delegate<Resource>> todo = new CompletableFuture<>();
        for (I_Decide<Resource> delegate : delegates) {
            Future<I_Delegate<Resource>> delegated = delegate.delegate(agent);
            log.info("command.delegated: {} @ {}", agent.getSelf(), delegated.getClass().getSimpleName());
            try {
                Resource resource = delegated.get().intent();
                Resource state = agent.getStateMachine().getState();
                if (resource != null && !resource.equals(state) && resource.isIRI()) {
                    log.info("command.escalated: {} @ {} -> {} --> {}", agent.getSelf(), state, resource, seen);
                    if (!seen.contains((IRI) resource))
                        todo.complete(() -> resource);
                    seen.add((IRI) resource);
                    break;
                }
            } catch (InterruptedException | ExecutionException e) {
                // no-op
            }
        }
        log.info("command.default: {} @ {}", agent.getSelf(), agent.getStateMachine().getState());
        todo.complete(() -> agent.getStateMachine().getState());
        return todo;
    }
}
