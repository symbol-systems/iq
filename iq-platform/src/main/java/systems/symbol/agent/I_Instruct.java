package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.intent.I_Intent;
import systems.symbol.model.HasIdentity;

/**
 * Interface defining the contract for an instruction within a symbolic system.
 * An instruction consists of a subject, an intent, and an object.
 */
public interface I_Instruct {

/**
 * Retrieves the subject of the instruction.
 *
 * @return The subject of the instruction as a Resource.
 */
Resource getSubject();

/**
 * Retrieves the action/verb of the instruction.
 *
 * @return The IRI of the action.
 */
IRI getAction();

/**
 * Retrieves the object of the instruction.
 *
 * @return The object of the instruction as a Resource.
 */
Resource getObject();
}
