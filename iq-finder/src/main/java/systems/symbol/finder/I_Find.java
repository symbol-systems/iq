package systems.symbol.finder;

import org.eclipse.rdf4j.model.IRI;

import java.util.Collection;
import java.util.List;

public interface I_Find<T> {
T find(String text);
}
