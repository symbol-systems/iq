package systems.symbol.model;

import org.eclipse.rdf4j.model.IRI;

/**
 * The {@code I_Self} interface represents entities that can self-identify.
 * Implementations return the canonical IRI for the entity.
 */
public interface I_Self {

/**
 * Retrieves the IRI representing the self-reference of the entity.
 *
 * @return The IRI representing the self-reference of the entity.
 */
IRI getSelf();
}
