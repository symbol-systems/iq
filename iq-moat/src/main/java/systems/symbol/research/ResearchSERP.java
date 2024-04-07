package systems.symbol.research;

import systems.symbol.annotation.RDF;
import systems.symbol.intent.AbstractIntent;
import systems.symbol.intent.IQIntent;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.IRIs;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import java.util.HashSet;
import java.util.Set;

public class ResearchSERP extends IQIntent {
    @Override
    @RDF(COMMONS.IQ_NS+"research-serp")
    public Set<IRI> execute(IRI subject, Resource object) {
        return new IRIs();
    }
}
