package systems.symbol.decide;

import systems.symbol.fsm.StateException;

import java.util.Stack;

public class StackDecision<T> implements I_Delegate<T> {
private final Stack<T> plan;

public StackDecision(Stack<T> plan) {
this.plan = plan;
}

@Override
public T intent() throws StateException {
return plan.pop();
}
}
