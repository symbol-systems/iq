package systems.symbol.rdf4j.sparql;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.util.UsefulSPARQL;

public class IQSimpleCatalog implements I_Contents {
Map<String, String> queries = new HashMap<>();

public IQSimpleCatalog() {
queries.put("a", UsefulSPARQL.TYPES_OF);
queries.put("skos", UsefulSPARQL.SKOS_CONCEPTS);
queries.put("subjects", UsefulSPARQL.SUBJECTS);
queries.put("predicates", UsefulSPARQL.PREDICATES);
queries.put("objects", UsefulSPARQL.OBJECTS);
queries.put("contexts", UsefulSPARQL.GRAPHS);

queries.put("index", UsefulSPARQL.INDEXER);
queries.put("iq-count", UsefulSPARQL.COUNT);
queries.put("iq-scripts", UsefulSPARQL.SCRIPTS); // rdf:value mimes
queries.put("iq-models", UsefulSPARQL.SPARQLS); // sparql mimes
queries.put("iq", UsefulSPARQL.META_ACTIONS);
queries.put("self", UsefulSPARQL.SELF);
}

public String getScript(String query) {
return queries.get(query);
}

@Override
public Literal getContent(Resource subject, IRI datatype) {
String query = queries.get(subject.stringValue());
if (query == null)
return null;
return Values.literal(query);
}
}