package systems.symbol.finder;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.util.Stopwatch;

public class IndexHelper {
    protected static final Logger log = LoggerFactory.getLogger(IndexHelper.class);

    public static long index(I_Finder finder, TupleQueryResult result) {
        long count = 0;
        Stopwatch stopwatch = new Stopwatch();
        // log.info("index.results ...");
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            String id = bindingSet.getValue("this").stringValue();

            StringBuilder s$ = new StringBuilder();
            for (String k : bindingSet.getBindingNames()) {
                if (!k.equals("this"))
                    s$.append(bindingSet.getValue(k).stringValue()).append(" ");
            }
            // log.debug("index.store: {} -> {}", id,
            // PrettyString.truncate(s$.toString(),16));
            finder.store(id, s$.toString());
            count++;
        }
        log.info("index.results: {} -> {} @ {}", finder, count, stopwatch);
        return count;
    }

    public static long index(I_Finder finder, TupleQuery query) {
        try (TupleQueryResult result = query.evaluate()) {
            return index(finder, result);
        }
    }

    public static long index(I_Finder finder, RepositoryResult<Statement> statements) {
        long count = 0;
        for (Statement s : statements) {
            finder.store(s.getSubject().stringValue(), s.getObject().stringValue());
            // log.info("indexed.fact: {} -> {}", s.getSubject().stringValue(),
            // s.getObject());
            count++;
        }
        return count;
    }

    public static long index(TextFinder finder, GraphQueryResult result) {
        long count = 0;
        while (result.hasNext()) {
            Statement s = result.next();
            finder.store(s.getSubject().stringValue(), s.getObject().stringValue());
            count++;
        }
        return count;
    }
}
