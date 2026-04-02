package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.repository.RepositoryConnection;

public class SPARQLRules {
private final RepositoryConnection connection;
private final String identity;

public SPARQLRules(RepositoryConnection connection, String identity) {
this.connection = connection;
this.identity = identity;
}

public int apply(String query) {
return 0;
}
}
