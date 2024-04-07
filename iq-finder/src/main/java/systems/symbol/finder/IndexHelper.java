package systems.symbol.finder;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

public class IndexHelper {

public static long index(I_Finder finder, TupleQueryResult result, String idField, String textField) {
long count = 0;
while (result.hasNext()) {
BindingSet bindingSet = result.next();
String id = bindingSet.getValue(idField).stringValue();
String text = bindingSet.getValue(textField).stringValue();
finder.store(id, text);
count++;
}
return count;
}

public static long index(I_Finder finder, TupleQuery query, String idField, String textField) {
try (TupleQueryResult result = query.evaluate()) {
return index(finder, result, idField, textField);
}
}
}
