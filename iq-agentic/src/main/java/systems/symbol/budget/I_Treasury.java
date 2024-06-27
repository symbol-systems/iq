package systems.symbol.budget;

import org.eclipse.rdf4j.model.IRI;

import java.math.BigDecimal;

public interface I_Treasury {
public I_Fund fund(IRI actor, IRI category, BigDecimal amount) throws BudgetException;
}
