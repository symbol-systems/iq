package systems.symbol.rdf4j.util;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Collections;

public class RDFCollections {
private final RepositoryConnection connection;

public RDFCollections(RepositoryConnection connection) {
this.connection = connection;
}

public boolean isList(Value value) {
return false;
}

public Collection<Value> getList(Resource resource) {
return Collections.emptyList();
}
}
