package systems.symbol.rdf4j.sparql;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;

import java.util.*;

public class QueryHelper {
    public static void setBindings(ValueFactory vf, TupleQuery query, Map<String, Object> args) {
        if (args == null) return;
        for (Map.Entry<String, Object> e : args.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (v == null) continue;
            if (v instanceof String && (((String) v).startsWith("http://") || ((String) v).startsWith("https://"))) {
                query.setBinding(k, vf.createIRI((String) v));
            } else if (v instanceof Integer) {
                query.setBinding(k, vf.createLiteral((Integer) v));
            } else {
                query.setBinding(k, vf.createLiteral(v.toString()));
            }
        }
    }

    public static Collection<Map<String, Object>> models(TupleQueryResult queryResult) throws TupleQueryResultHandlerException {
        Collection<Map<String, Object>> out = new ArrayList<>();
        try (TupleQueryResult res = queryResult) {
            while (res.hasNext()) {
                BindingSet bs = res.next();
                Map<String, Object> m = new HashMap<>();
                bs.getBindingNames().forEach(name -> {
                    var val = bs.getValue(name);
                    if (val instanceof IRI) m.put(name, val.stringValue());
                    else if (val instanceof Literal) m.put(name, ((Literal) val).getLabel());
                    else m.put(name, val==null?null:val.stringValue());
                });
                out.add(m);
            }
        }
        return out;
    }
}