package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;

/**
 * The {@code I_Self} interface represents entities that can self-identify.
 * Implementations return the canonical IRI for the entity.
 */
public interface I_Self {

        IRI getSelf();

        interface I_Contents {
            Literal getContent(Resource subject, IRI datatype);

        }
}
