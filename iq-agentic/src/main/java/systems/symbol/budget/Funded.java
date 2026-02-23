package systems.symbol.budget;

import org.eclipse.rdf4j.model.IRI;

import java.math.BigDecimal;

class Funded implements I_Fund {
Budget budget;
IRI actor, category;
BigDecimal balance;

public Funded(Budget budget, IRI actor, IRI category, BigDecimal balance) {
this.budget = budget;
this.actor = actor;
this.category = category;
this.balance = balance;
}

public IRI getActor() {
return this.actor;
}
public IRI spend(IRI expense, BigDecimal amount) throws BudgetException {
return expense;
}

public void done() {
budget.amount.add(balance);
balance = BigDecimal.valueOf(0.0);
}

public IRI getCategory() {
return category;
}

public BigDecimal getBalance() {
return balance;
}
}