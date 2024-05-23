package systems.symbol.trust;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import systems.symbol.intent.ExecutiveIntent;

public class TrustedIntent extends ExecutiveIntent {
public TrustedIntent(IRI self, Model model) {
super(self, model);
}
}
