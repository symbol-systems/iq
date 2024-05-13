package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import systems.symbol.fsm.StateException;

/**
 * Interface for bootstrapping the IQ operating environment.
 *
 * The bootstrap process initializes the IQ environment, setting up essential components
 * and configurations necessary for operation.
 *
 * Implementations of this interface define the contract for bootstrapping the IQ environment
 * using the provided self identifier (IRI) and RDF model containing initial configuration
 * and data.
 *
 * The bootstrap process may involve initializing agents, loading knowledge graphs, configuring
 * state machines, and other setup tasks required to prepare the IQ environment for symbolic cognition.
 *
 * If an error occurs during the bootstrap process, implementations can throw a StateException
 * to signal the error condition.
 */
public interface I_Bootstrap {
    /**
     * Boots up the IQ environment.
     *
     * @param self  The self identifier (IRI) representing the environment.
     * @param model The RDF model containing initial configuration and data.
     * @throws StateException if an error occurs during the bootstrap process.
     */
    void boot(IRI self, Model model) throws StateException;
}
