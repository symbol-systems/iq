package systems.symbol.trust;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public class CREATOR extends TrustedIntent {
    public CREATOR(IRI self, Model model) {
        super(self, model);
    }
}
