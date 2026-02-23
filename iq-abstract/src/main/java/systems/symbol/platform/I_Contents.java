package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;

/**
 * Interface for accessing text-based assets in IQ.
 *
 * Content may represent information, serialized data, or resources stored within the IQ environment,
 * accessible through any means.
 *
 * Implementations of this interface define the contract for accessing content associated
 * with a given subject and data type (IRI). The getContent() method retrieves the content
 * associated with the specified subject and data type, returning a Literal representation
 * of the content.
 */
public interface I_Contents {
        /**
         * Retrieves the content associated with the specified subject and data type.
         *
         * @param subject  The subject of the content.
         * @param datatype The data type (IRI) of the content.
         * @return The Literal representation of the content.
         */
        Literal getContent(Resource subject, IRI datatype);
}
