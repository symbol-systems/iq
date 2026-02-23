package systems.symbol.gql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
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

    private final String typeIRI;
    private final PolicyEngine policyEngine;
    private final DataFetcher<Collection<Map<String,Object>>> delegate;

    public AuthorizationDataFetcher(String typeIRI, PolicyEngine policyEngine, DataFetcher<Collection<Map<String,Object>>> delegate) {
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

        boolean allowed = policyEngine.isAllowed(actor, typeIRI, environment);

        if (!allowed) {
            log.info("Access denied for actor {} on type {}", actor, typeIRI);
            throw new SecurityException("Not authorized");
        }

        // authorized — delegate
        return delegate.get(environment);
    }
}
