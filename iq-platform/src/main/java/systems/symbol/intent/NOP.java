package systems.symbol.intent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.COMMONS;
import systems.symbol.RDF;
import systems.symbol.platform.I_Self;

import javax.script.Bindings;
import java.util.HashSet;
import java.util.Set;

public class NOP implements I_Intent, I_Self {
    static Set<IRI> nop = new HashSet<>();
    @Override
    @RDF(COMMONS.IQ_NS+"nop")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
        return nop;
    }

    @Override

    public IRI getSelf() {
        return Values.iri("urn:"+getClass().getCanonicalName());
    }

}
