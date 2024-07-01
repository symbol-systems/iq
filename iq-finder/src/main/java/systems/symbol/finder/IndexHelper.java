package systems.symbol.finder;

import dev.langchain4j.data.embedding.Embedding;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexHelper {
protected static final Logger log = LoggerFactory.getLogger(IndexHelper.class);

public static long index(I_Finder finder, TupleQueryResult result) {
long count = 0;
while (result.hasNext()) {
BindingSet bindingSet = result.next();
String id = bindingSet.getValue("this").stringValue();

StringBuilder s$ = new StringBuilder();
for(String k: bindingSet.getBindingNames()) {
if (!k.equals("this"))
s$.append( bindingSet.getValue(k).stringValue()).append(" ");
}

log.debug("indexing: {} -> {}", id, s$);
finder.store(id, s$.toString());
count++;
}
log.info("indexed.results: {}",count);
return count;
}

public static long index(I_Finder finder, TupleQuery query) {
try (TupleQueryResult result = query.evaluate()) {
return index(finder, result);
}
}

public static long index(I_Finder finder, RepositoryResult<Statement> statements) {
long count = 0;
for(Statement s: statements) {
Embedding store = finder.store(s.getSubject().stringValue(), s.getObject().stringValue());
//log.info("indexed.fact: {} -> {}", s.getSubject().stringValue(), s.getObject());
count++;
}
return count;
}
}
