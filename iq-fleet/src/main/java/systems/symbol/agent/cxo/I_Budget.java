package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.model.I_Self;

import java.math.BigDecimal;

public interface I_Budget {
    public IRI getCategory();
    public BigDecimal getBalance();
}