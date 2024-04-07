package systems.symbol.intent;

import systems.symbol.fsm.StateException;
import systems.symbol.model.HasIdentity;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import java.util.Set;

/**
 * Interface for an intent within a symbolic system.
 * Extends the HasIdentity interface because each intent has a unique identity.
 */
public interface I_Intent extends HasIdentity {

    /**
     * Executes the intent based on the provided subject and object.
     *
     * @param subject The subject of the execution.
     * @param object  The object of the execution.
     * @return A set of IRIs resulting from the execution.
     * @throws StateException If an error occurs during execution.
     */
    Set<IRI> execute(IRI subject, Resource object) throws StateException;
}
