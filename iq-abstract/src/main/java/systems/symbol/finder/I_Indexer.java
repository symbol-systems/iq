package systems.symbol.finder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Iterator;

public interface I_Indexer {
    void reindex(Iterator<Statement> facts, IRI concept);
}
