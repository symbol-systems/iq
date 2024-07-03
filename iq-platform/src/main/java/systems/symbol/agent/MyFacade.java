package systems.symbol.agent;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.util.IdentityHelper;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyFacade {
protected final static Logger log = LoggerFactory.getLogger(MyFacade.class);

public static final String SELF = "self";
public static final String STATE = "state";
public static final String RESULTS = "results";
public static final String INTENT = "intent";
public static final String NAME = "name" ;
public static final String MY = "my";
public static final String IQ = "iq";
public static final String AI = "ai";
public static final String CONTENT = "content";
public static final String TIME = "time";
private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
private static final DateFormat humanDateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy HH:mm:ss");

public static List<Map<String, Object>> results(Bindings bindings, List<Map<String, Object>> results) {
Object o = bindings.get(MY);
if (!(o instanceof Map)) return null;
@SuppressWarnings("unchecked")
Map<String, Object> my = (Map<String, Object>) o;
my.put(RESULTS, results);
return results;
}

public static Bindings rebind(IRI self, Resource state, @NotNull Bindings my) {
Bindings bindings = rebind(self, my);
my.put(INTENT, IdentityHelper.uuid(self+"#"));
if (state!=null) my.put(STATE, state.stringValue());
return bindings;
}

public static Bindings rebind(IRI self, @NotNull Bindings my) {
Bindings bindings;
if (my.containsKey(MY)) {
bindings = my;
my = (Bindings) bindings.get(MY);
log.debug("my.rebind: {} -> {}", my.keySet(), bindings.keySet());
} else {
bindings = new SimpleBindings();
bindings.put(MY, my);
log.debug("my.bind: {}", my.keySet());
}
my.put(TIME, humanDateFormat.format(new Date()));
my.put(SELF, self.stringValue());
return bindings;
}

public static Bindings trust(IRI agent, Resource state, Model model, @NotNull Bindings my, I_Secrets trusted) throws SecretsException {
IQFacade facade = new IQFacade(agent, model, trusted);
try {
facade.enableVFS();
} catch (FileSystemException e) {
// ignore
}
return trust(agent,state,my,facade);
}

public static Bindings trust(IRI agent, Resource state,  @NotNull Bindings my, IQFacade facade) throws SecretsException {
log.debug("iq.trust: {} @ {} --> {}", agent, state, my.keySet());
Bindings bindings = rebind(agent, state, my);
bindings.put(IQ, facade);
log.debug("iq.bound: {}", bindings.keySet());
return bindings;
}


public static void dump(Map<?,?> bindings, OutputStream out) {
PrintWriter writer = new PrintWriter(out);
writer.println("------");
if (bindings!=null)
for (Map.Entry<?,?> entry : bindings.entrySet()) {
writer.printf("\t%s == ", entry.getKey());

Object value = entry.getValue();
if (value instanceof List) {
List<?> values = (List<?>) value;
for (Object value1 : values) {
writer.printf("- %s ", value1);
}
} else if ((value instanceof Map)) {
Map<?,?> temp = (Map<?,?>) value;
writer.printf("%s\n", temp.keySet());
writer.printf("\n\t\t[%s] => %s", entry.getKey(), temp.keySet());
dump(temp, out);
} else {
writer.printf("%s (%s)", value, value==null?"NULL":value.getClass().getCanonicalName());
}
writer.println();
}

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

public static Bindings bind(@NotNull Bindings bindings, MultivaluedMap<String, String> queryParameters) {
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

public static Bindings rebind(IRI self, @NotNull Bindings params, DecodedJWT jwt) {
Bindings my = rebind(self, params);
my.put("jwt", jwt);
return my;
}
}
