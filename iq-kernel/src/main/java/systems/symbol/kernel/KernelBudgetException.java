package systems.symbol.kernel;

/**
 * Thrown when a budget or quota limit is exceeded.
 * Replaces {@code BudgetException} for cross-surface use.
 */
public class KernelBudgetException extends KernelException {

public KernelBudgetException(String message) {
super("kernel.budget.exceeded", message);
}

public KernelBudgetException(String code, String message) {
super(code, message);
}

public KernelBudgetException(String message, Throwable cause) {
super("kernel.budget.exceeded", message, cause);
}
}
