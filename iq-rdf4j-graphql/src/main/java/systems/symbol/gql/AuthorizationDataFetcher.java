package systems.symbol.gql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Wrapper that enforces a simple ASK-based ACL before delegating to an inner DataFetcher.
 *
 * Default ASK template:
 * ASK WHERE { <actor> <http://symbol.systems/v0/onto/trust#canQuery> <typeIRI> }
 */
public class AuthorizationDataFetcher implements DataFetcher<Collection<Map<String,Object>>> {
private static final Logger log = LoggerFactory.getLogger(AuthorizationDataFetcher.class);

private static final String CAN_QUERY = "http://symbol.systems/v0/onto/trust#canQuery";

private final Repository repository;
private final String typeIRI;
private final PolicyEngine policyEngine;
private final DataFetcher<Collection<Map<String,Object>>> delegate;

public AuthorizationDataFetcher(Repository repository, String typeIRI, PolicyEngine policyEngine, DataFetcher<Collection<Map<String,Object>>> delegate) {
this.repository = repository;
this.typeIRI = typeIRI;
this.policyEngine = policyEngine;
this.delegate = delegate;
}

@Override
public Collection<Map<String, Object>> get(DataFetchingEnvironment environment) throws Exception {
// Determine actor (try argument "actor" first, then context map key "actor")
String actor = null;
try {
Object a = environment.getArgument("actor");
if (a!=null) actor = a.toString();
} catch (Exception ignored) {}

if (actor==null) {
Object ctx = environment.getContext();
if (ctx instanceof Map) {
Object a = ((Map)ctx).get("actor");
if (a!=null) actor = a.toString();
}
}

if (actor==null) {
log.debug("No actor provided in environment; denying by default");
throw new SecurityException("Not authorized: no actor");
}

// Explicit canQuery grants should short-circuit the policy engine.
try {
var vf = repository.getValueFactory();
IRI actorIri = vf.createIRI(actor);
IRI typeIri = vf.createIRI(typeIRI);
IRI canQuery = vf.createIRI(CAN_QUERY);
try (var conn = repository.getConnection()) {
if (conn.hasStatement(actorIri, canQuery, typeIri, true)) {
log.info("Access allowed for actor {} on type {} via explicit canQuery", actor, typeIRI);
return delegate.get(environment);
}
}
} catch (Exception e) {
log.debug("Explicit canQuery check failed for actor {} on type {}: {}", actor, typeIRI, e.getMessage());
}

boolean allowed = policyEngine.isAllowed(actor, typeIRI, environment);

if (!allowed) {
log.info("Access denied for actor {} on type {}", actor, typeIRI);
throw new SecurityException("Not authorized");
}

// authorized — delegate
return delegate.get(environment);
}
}
