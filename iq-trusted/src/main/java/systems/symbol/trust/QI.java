package systems.symbol.trust;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import systems.symbol.RDF;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;

import javax.script.Bindings;
import java.util.Set;

// knowledge lake
public class QI extends TrustedIntent {

public QI(IRI self, Model model) {
super(self, model);
}

@RDF(IQ_NS.IQ + "do")
@Override
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) throws StateException {
return super.execute(actor, state, bindings);
}
}
