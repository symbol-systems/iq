package systems.symbol.budget;

import org.eclipse.rdf4j.model.IRI;

import java.math.BigDecimal;

public interface I_Fund {
    public IRI spend(IRI expense, BigDecimal amount) throws BudgetException;
    public void done();
}