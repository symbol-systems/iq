package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.cxo.BudgetException;

import java.math.BigDecimal;

public interface I_Treasury {
    public systems.symbol.agent.I_Fund fund(IRI actor, IRI category, BigDecimal amount) throws BudgetException;
}
