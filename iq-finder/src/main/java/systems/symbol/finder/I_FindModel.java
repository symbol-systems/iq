package systems.symbol.finder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import java.util.List;

public interface I_FindModel extends I_Find<Model> {
    Model find(String text);
}
