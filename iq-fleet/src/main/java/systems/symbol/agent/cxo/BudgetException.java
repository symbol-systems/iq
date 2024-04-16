package systems.symbol.agent.cxo;

public class BudgetException extends Exception {

    public BudgetException(String msg, Exception e) {
        super(e.getMessage());
    }
}
