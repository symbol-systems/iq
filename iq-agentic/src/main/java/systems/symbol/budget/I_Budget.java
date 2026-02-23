package systems.symbol.budget;

import org.eclipse.rdf4j.model.IRI;

import java.math.BigDecimal;

public interface I_Budget {
    public IRI getCategory();
    public BigDecimal getBalance();
}