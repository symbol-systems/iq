package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import systems.symbol.fsm.StateException;

public interface I_Bootstrap {
    void boot(IRI self, Model model) throws StateException;
}
