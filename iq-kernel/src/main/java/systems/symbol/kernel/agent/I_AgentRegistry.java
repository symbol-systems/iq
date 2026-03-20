package systems.symbol.kernel.agent;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.I_Agent;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for agent instances managed by Kernel.
 */
public interface I_AgentRegistry {

    /** Register an agent. If agent with same IRI exists, overwrite and return previous. */
    Optional<I_Agent> register(I_Agent agent);

    /** Get agent by IRI. */
    Optional<I_Agent> lookup(IRI agentId);

    /** Remove agent by IRI, returning removed agent if any. */
    Optional<I_Agent> unregister(IRI agentId);

    /** List all registered agents. */
    Collection<I_Agent> list();

    /** Number of registered agents. */
    int size();
}
