package systems.symbol.agent;

import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MyFacade {
public static final String SELF = "self";
public static final String STATE = "state";
public static final String RESULTS = "results";
public static final String INTENT = "intent";
public static final String MY = "my";
public static final String IQ = "iq";
public static final String PROMPT = "prompt";
public static final String TIME = "time";
private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

public static List<Map<String, Object>> results(Bindings bindings, List<Map<String, Object>> results) {
Object o = bindings.get(MY);
if (!(o instanceof Map)) return null;
@SuppressWarnings("unchecked")
Map<String, Object> my = (Map<String, Object>) o;
my.put(RESULTS, results);
return results;
}

public static Bindings rebind(IRI self, Resource state, @NotNull Bindings my) {
my.put(INTENT, IdentityHelper.uuid(self+"#"));
my.put(STATE, state.stringValue());
return rebind(self, my);
}

public static Bindings rebind(IRI self, @NotNull Bindings my) {
Bindings bindings;
if (my.containsKey(MY) || my.containsKey(IQ)) {
bindings = my;
} else bindings = new SimpleBindings();

bindings.put(MY, my);
my.put(TIME, dateFormat.format(new Date()));
my.put(SELF, self.stringValue());
return bindings;
}

public static Bindings rebind(IRI agent, Resource state, Model model, Bindings my, I_Secrets secrets) {
Bindings bindings = rebind(agent, state, my);
IQFacade facade = new IQFacade(agent, model, secrets);
try {
facade.enableVFS();
} catch (FileSystemException e) {
// ignore
}
bindings.put(IQ, facade);
return bindings;
}

}
