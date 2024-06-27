package systems.symbol.budget;

import org.eclipse.rdf4j.model.IRI;

import java.math.BigDecimal;

public class Budget  {
    IRI treasurer, category;
    BigDecimal amount;

    public Budget(IRI treasurer, IRI category, BigDecimal amount) {
        this.treasurer = treasurer;
        this.category = category;
        this.amount = amount;
    }

    public I_Fund fund(IRI actor, IRI category, BigDecimal amount) throws BudgetException {
//        if (!approved(actor, category, amount)) throw new BudgetException("oops);
//        this.amount = this.amount - amount;
        return new Funded(this, actor, category, amount);
    }
}

