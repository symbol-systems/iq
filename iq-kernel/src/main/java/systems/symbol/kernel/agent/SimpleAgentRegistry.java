package systems.symbol.kernel.agent;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.I_Agent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe default agent registry.
 */
public class SimpleAgentRegistry implements I_AgentRegistry {

    private final Map<String, I_Agent> agents = new ConcurrentHashMap<>();

    @Override
    public Optional<I_Agent> register(I_Agent agent) {
        if (agent == null) {
            return Optional.empty();
        }
        IRI id = agent.getSelf();
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.put(id.stringValue(), agent));
    }

    @Override
    public Optional<I_Agent> lookup(IRI agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.get(agentId.stringValue()));
    }

    @Override
    public Optional<I_Agent> unregister(IRI agentId) {
        if (agentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(agents.remove(agentId.stringValue()));
    }

    @Override
    public Collection<I_Agent> list() {
        return Collections.unmodifiableCollection(agents.values());
    }

    @Override
    public int size() {
        return agents.size();
    }
}
