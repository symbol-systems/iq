/**
 * Interface defining the contract for an instruction in IQ.
 *
 * An instruction represents an action or command to be executed by an agent within the IQ system.
 * It consists of three main components: the agent/actor, the action verb, and the target.
 *
 * This interface provides methods to encode an executable instruction.
 */

package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

public interface I_Command {

/**
 * Retrieves the agent/actor (RDF subject) of the instruction.
 *
 * @return The subject of the instruction as an IRI.
 */
IRI getActor();

/**
 * Retrieves the action verb (RDF predicate) of the instruction.
 *
 * @return The IRI representing the action.
 */
IRI getAction();

/**
 * Retrieves the target (RDF object) of the instruction.
 *
 * @return The target of the instruction as a Resource.
 */
Resource getTarget();
}
