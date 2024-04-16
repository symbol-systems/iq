package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

/**
 * Interface defining the contract for an instruction within a symbolic system.
 * An instruction consists of a subject, an intent, and an object.
 */
public interface I_Instruct {

/**
 * The actor of the instruction.
 *
 * @return The subject of the instruction as an IRI.
 */
IRI getActor();

/**
 * The action/verb of the instruction.
 *
 * @return The IRI representing the action.
 */
IRI getAction();

/**
 * The target of the instruction.
 *
 * @return The target of the instruction as a Resource.
 */
Resource getTarget();
}
