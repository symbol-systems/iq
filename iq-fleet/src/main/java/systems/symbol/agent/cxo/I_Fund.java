package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.agent.cxo.BudgetException;
import systems.symbol.model.I_Self;

import java.math.BigDecimal;

public interface I_Fund {
public IRI spend(IRI expense, BigDecimal amount) throws BudgetException;
public void done();
}