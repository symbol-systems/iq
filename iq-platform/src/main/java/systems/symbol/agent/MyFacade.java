package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.fsm.StateException;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.List;
import java.util.Map;

public class MyFacade {
public static final String SELF = "self";
public static final String STATE = "state";
public static final String RESULTS = "results";
public static final String INTENT = "intent";
public static final String MY = "my";
public static final String IQ = "iq";

public static Bindings rebind(I_Agent agent) throws StateException {
return rebind(agent, new SimpleBindings());
}

public static Bindings rebind(I_Agent agent, Map<String,Object> my) throws StateException {
Bindings bindings = rebind(agent.getSelf(),my);
bindings.put(STATE, agent.getStateMachine().getState());
bindings.put(IQ, new IQFacade(agent.getSelf(), agent.getMemo()));
return bindings;
}

public static void results(Bindings bindings, List<Map<String, Object>> results) {
Object o = bindings.get(MY);
if (!(o instanceof Map)) return;
@SuppressWarnings("unchecked")
Map<String, Object> my = (Map<String, Object>) o;
my.put(RESULTS, results);
}

public static Bindings rebind(IRI self, Resource state, @NotNull Map<String,Object> my) {
Bindings bindings = rebind(self, my);
bindings.put(STATE, state);
return bindings;
}

public static Bindings rebind(IRI self, @NotNull Map<String,Object> my) {
Bindings bindings = new SimpleBindings();
if (my.containsKey(MY) || my.containsKey(IQ)) {
bindings.putAll(my);
//noinspection unchecked
my = (Map<String, Object>) bindings.get(MY);
}
bindings.put(MY, my);
my.put(SELF, self);
return bindings;
}

}
