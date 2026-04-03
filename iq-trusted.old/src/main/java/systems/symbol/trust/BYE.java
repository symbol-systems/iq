package systems.symbol.trust;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;

import javax.script.Bindings;
import java.util.HashSet;
import java.util.Set;

public class BYE extends TrustedIntent {
public BYE(IRI self, Model model) {
super(self, model);
}

@RDF(IQ_NS.IQ + "bye")
@Override
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
System.exit(999);
return new HashSet<>();
}
}
