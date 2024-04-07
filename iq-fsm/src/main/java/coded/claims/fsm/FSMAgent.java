package systems.symbol.fsm;

import org.apache.camel.Message;
import org.jeasy.states.api.*;
import org.jeasy.states.core.FiniteStateMachineBuilder;
import org.jeasy.states.core.TransitionBuilder;

import java.util.HashSet;
import java.util.Set;

public class FSMAgent {
Set<State> states = new HashSet<>();
Set<Transition> transitions = new HashSet();

FiniteStateMachine fsm(State initial) {
FiniteStateMachineBuilder builder = new FiniteStateMachineBuilder(states, initial);
builder.registerTransitions(transitions);
return builder.build();
}

State state(String name) {
State state = new State(name);
states.add(state);
return state;
}

Transition transition(String name, State from, State to) {
return transition(name,from,to, null);
}

Transition transition(String name, State from, State to, EventHandler event) {
TransitionBuilder builder = new TransitionBuilder()
.name(name)
.sourceState(from)
.eventType(FSMActionEvent.class)
.targetState(to);
if (event!=null) builder.eventHandler(event);
return builder.build();
}
}

class FSMAgentActionHandler implements EventHandler<FSMActionEvent> {

@Override
public void handleEvent(FSMActionEvent action) throws Exception {
System.out.println("iq.camel.fsm.handleEvent:"+action.getName());
}
}
class FSMActionEvent implements Event {
Message msg;

FSMActionEvent(Message msg) {
this.msg = msg;
}

@Override
public String getName() {
return msg.getMessageId();
}

@Override
public long getTimestamp() {
return msg.getMessageTimestamp();
}
}