package systems.symbol.gql;

import graphql.schema.DataFetchingEnvironment;

public interface PolicyEngine {
/**
 * Determine whether an actor is allowed to query a given type/resource.
 * @param actor IRI of the actor (http://...)
 * @param typeIRI IRI of the type/resource being queried
 * @param env DataFetchingEnvironment for additional context
 * @return true if allowed
 */
boolean isAllowed(String actor, String typeIRI, DataFetchingEnvironment env);
}
