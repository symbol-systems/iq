package systems.symbol.model;

import systems.symbol.rdf4j.NS;
import org.eclipse.rdf4j.model.IRI;

import java.util.HashMap;
import java.util.Map;

public class NamedMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 2462762779494608931L;

    public NamedMap(IRI iri) {
        this(iri,null);
    }

    public NamedMap(IRI iri, Map <String, Object> attr) {
        if (attr!=null) putAll(attr);
        put(NS.KEY_AT_ID, iri);
    }
    public NamedMap(Map <String, Object> attr) {
        if (attr!=null) putAll(attr);
    }

    public IRI getIdentity() {
        return (IRI)get(NS.KEY_AT_ID);
    }

    public static IRI getIdentity(Map<String, Object> m) {
        return (IRI)m.get(NS.KEY_AT_ID);
    }
}
