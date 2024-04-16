package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;

public interface I_Contents {

    Literal getContent(Resource subject, IRI datatype);

}
