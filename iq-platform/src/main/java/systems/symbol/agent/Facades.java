package systems.symbol.agent;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.render.HBSRenderer;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyStrings;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Facades {
protected final static Logger log = LoggerFactory.getLogger(Facades.class);

public static final String SELF = "self";
public static final String FOCUS = "focus";
public static final String INTENTS = "intents";
public static final String RESULTS = "results";
public static final String NAME = "name";
public static final String TIME = "time";
public static final String AGENT = "agent";
public static final String TODAY = "today";

public static final String MY = "my";
public static final String IQ = "iq";
public static final String AI = "ai";
// private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd
// HH:mm:ss");
private static final DateFormat humanDateFormat = new SimpleDateFormat("EEEE, MM dd, yyyy HH:mm:ss");

public static List<Map<String, Object>> results(Bindings bindings, List<Map<String, Object>> results) {
Object o = bindings.get(MY);
if (!(o instanceof Map))
return new ArrayList<>();
@SuppressWarnings("unchecked")
Map<String, Object> my = (Map<String, Object>) o;
my.put(RESULTS, results);
return results;
}

public static Bindings rebind(IRI self, Resource state, Bindings my) {
Bindings bindings = rebind(self, my);
if (!my.containsKey(SELF))
my.put(SELF, IdentityHelper.uuid(self + "#"));
return bindings;
}

public static Bindings rebind(IRI self, Bindings my) {
Bindings bindings;
if (my.containsKey(MY)) {
bindings = my;
my = (Bindings) bindings.get(MY);
} else {
bindings = new SimpleBindings();
bindings.put(MY, my);
}
// my.put(TIME, time());
my.put(SELF, self.stringValue());
return bindings;
}

public static String time() {
return humanDateFormat.format(new Date());
}

public static Bindings facade(IRI agent, Resource state, Model model, Bindings my, I_Secrets trusted)
throws SecretsException {
IQFacade facade = new IQFacade(agent, model, trusted);
try {
facade.enableVFS();
} catch (FileSystemException e) {
// ignore
}
// return iq(agent, state, my, facade);
// }

// public static Bindings iq(IRI agent, Resource state, Bindings my, IQFacade
// facade)
// throws SecretsException {
log.debug("facade.iq: {} @ {} --> {}", agent, state, my.keySet());
Bindings bindings = rebind(agent, state, my);
bindings.put(IQ, facade);
log.debug("facade.bound: {}", bindings.keySet());
return bindings;
}

public static void dump(Map<?, ?> bindings, OutputStream out) {
PrintWriter writer = new PrintWriter(out);
writer.println("~ ~ ~ ~");
writer.println(PrettyStrings.pretty(bindings));
writer.println("~ ~ ~ ~");
writer.flush();
writer.close();
}

public static Map<String, String> toMap(UriInfo uriInfo) {
Map<String, String> queryParamsMap = new HashMap<>();
MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

for (String key : queryParams.keySet()) {
List<String> values = queryParams.get(key);
if (values != null && !values.isEmpty()) {
queryParamsMap.put(key, values.get(0));
}
}

return queryParamsMap;
}

public static Bindings bind(Bindings bindings, MultivaluedMap<String, String> queryParameters) {
for (Map.Entry<String, java.util.List<String>> entry : queryParameters.entrySet()) {
String key = entry.getKey();
java.util.List<String> values = entry.getValue();

if (values == null || values.isEmpty()) {
bindings.put(key, "");
} else if (values.size() == 1) {
bindings.put(key, values.get(0));
} else {
bindings.put(key, values);
}
}
return bindings;
}

public static Bindings bind(MultivaluedMap<String, String> queryParameters) {
return bind(new SimpleBindings(), queryParameters);
}

public static Bindings rebind(IRI self, Bindings params, DecodedJWT jwt) {
Bindings my = rebind(self, params);
my.put("jwt", jwt);
return my;
}

public static String template(String template, Map<String, Object> bindings) {
try {
return template == null || bindings == null ? template : HBSRenderer.template(template, bindings);
} catch (IOException e) {
return template;
}
}
}
