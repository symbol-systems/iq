package systems.symbol.intent;

import systems.symbol.annotation.RDF;
import org.eclipse.rdf4j.model.IRI;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.ns.COMMONS;

public class NOP implements I_Intent {
    static Set<IRI> nop = new HashSet<>();
    @Override
    @RDF(COMMONS.IQ_NS+"nop")
    public Set<IRI> execute(IRI subject, Resource object) {
        return nop;
    }

    public IRI getIdentity() {
        return Values.iri("urn:"+getClass().getCanonicalName());
    }

}
