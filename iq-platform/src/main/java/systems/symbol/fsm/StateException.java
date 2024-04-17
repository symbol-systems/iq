package systems.symbol.fsm;


public class StateException extends Exception {
public Object state;
public StateException(String s, Object state) {
super(s);
this.state = state;
}

public StateException(String s, Object state, Exception e) {
super(s ,e);
this.state = state;
}
}
