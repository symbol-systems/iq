package systems.symbol.gql;

import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * Chains multiple PolicyEngine instances. If any engine allows, the chain allows.
 * If none allows, returns false.
 */
public class PolicyEngineChain implements PolicyEngine {
    final List<PolicyEngine> engines = new ArrayList<>();

    public PolicyEngineChain() {}

    public void add(PolicyEngine engine) { if (engine!=null) engines.add(engine); }

    @Override
    public boolean isAllowed(String actor, String typeIRI, DataFetchingEnvironment env) {
        for (PolicyEngine p: engines) {
            try {
                if (p.isAllowed(actor, typeIRI, env)) return true;
            } catch (Exception e) {
                // ignore and try next
            }
        }
        return false;
    }
}
