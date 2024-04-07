package systems.symbol.core;

import systems.symbol.model.HasIdentity;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Map;

public interface IRIModel extends Map<IRI, Value>, HasIdentity {
}
