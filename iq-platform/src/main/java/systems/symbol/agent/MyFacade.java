package systems.symbol.agent;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.fsm.StateException;
import systems.symbol.secrets.I_Secrets;

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

//public static Bindings rebind(I_Agent agent) throws StateException {
//return rebind(agent, new SimpleBindings());
//}

public static void results(Bindings bindings, List<Map<String, Object>> results) {
Object o = bindings.get(MY);
if (!(o instanceof Map)) return;
@SuppressWarnings("unchecked")
Map<String, Object> my = (Map<String, Object>) o;
my.put(RESULTS, results);
}

public static Bindings rebind(IRI self, Resource state, @NotNull Bindings my) {
Bindings bindings = rebind(self, my);
bindings.put(STATE, state);
return bindings;
}

public static Bindings rebind(IRI self, @NotNull Bindings my) {
Bindings bindings;
if (my.containsKey(MY) || my.containsKey(IQ)) {
bindings = my;
} else bindings = new SimpleBindings();

bindings.put(MY, my);
my.put(SELF, self);
return bindings;
}

public static Bindings rebind(IRI self, Model model, Bindings my, I_Secrets secrets) throws StateException {
Bindings bindings = MyFacade.rebind(self, my);
bindings.put(MyFacade.IQ, new IQFacade(self, model, secrets));
return bindings;
}

public static Bindings rebind(I_Agent agent, Bindings my, I_Secrets secrets) throws StateException {
Bindings bindings = rebind(agent.getSelf(),my);
bindings.put(STATE, agent.getStateMachine().getState());
bindings.put(IQ, new IQFacade(agent.getSelf(), agent.getMemo(), secrets));
return bindings;
}

}
