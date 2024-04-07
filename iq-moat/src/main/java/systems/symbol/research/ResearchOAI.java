package systems.symbol.research;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.annotation.RDF;
import systems.symbol.intent.IQIntent;
import systems.symbol.ns.COMMONS;

import java.util.Set;

public class ResearchOAI extends IQIntent {
@Override
@RDF(COMMONS.IQ_NS+"research-oai")
public Set<IRI> execute(IRI subject, Resource object) {
return null;
}
}
