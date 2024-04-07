package systems.symbol.io;

import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.iq.IQ;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.rdf4j.sparql.SPARQLMapper;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Display {
protected static final Logger log = LoggerFactory.getLogger(Display.class);

public static void display(List<Map<String,Object>> models) {
if (models==null) return;
models.forEach(Display::display);
}

public static void display(Map<String,Object> model) {
Object label = model.get("label");
if (label!=null) {
System.out.println("> " + label+" @ "+model.get(NS.KEY_AT_ID));
} else {
System.out.println("> " + model);
}

}
public static void table(PrintStream out, List<Map<String,Object>> models) {
table(out, models, "%30s %50s\n", new String[]{"@id", "label"});
}

public static void table(PrintStream out, List<Map<String, Object>> models, String format, String[] fields) {
// Print header
for (String field : fields) {
out.printf(format, field);
}
out.println();

// Print rows
for (Map<String, Object> model : models) {
for (String field : fields) {
Object value = model.get(field);
out.printf(format, value != null ? value : "");
}
out.println();
}
}


public static List<Map<String,Object>> models(IQ iq, String queryPath) {
queryPath = normalizeQuery(queryPath);
IRI queryIRI = iq.toIRI(queryPath);
// log.info("iq.models.sparql: {} as {}" , queryPath, queryIRI);
SPARQLMapper models = new SPARQLMapper(iq);
return models.models(queryIRI);
}

public static String normalizeQuery(String query) {
if (!query.contains(".")) {
query = query+".sparql";
}
if (!query.contains("/")) {
query = "queries/"+query;
}
return query;
}


}
