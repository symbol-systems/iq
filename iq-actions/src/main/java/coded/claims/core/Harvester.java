package systems.symbol.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;

import java.util.Map;

public interface Harvester {

    public TripleSource harvest(IRI iri, Map<String,Object> attr);
}
