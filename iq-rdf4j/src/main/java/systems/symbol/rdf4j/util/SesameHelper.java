package systems.symbol.rdf4j.util;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class SesameHelper {

public static Collection<Map<String, Object>> toMapCollection(RepositoryConnection connection, String sparql) {
return new ArrayList<>();
}

public static ValueFactory getValueFactory() {
return SimpleValueFactory.getInstance();
}
}
