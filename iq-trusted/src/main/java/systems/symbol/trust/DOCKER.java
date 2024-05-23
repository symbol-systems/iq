package systems.symbol.trust;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

public class DOCKER extends TrustedIntent {
    public DOCKER(IRI self, Model model) {
        super(self, model);
    }
}
