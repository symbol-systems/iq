package systems.symbol.agent.budget;

public class BudgetException extends Exception {

public BudgetException(String msg, Exception e) {
super(e.getMessage());
}
}
