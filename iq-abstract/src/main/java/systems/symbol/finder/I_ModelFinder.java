package systems.symbol.finder;

import org.eclipse.rdf4j.model.Model;

/**
 * Interface for semantic fact finders within IQ.
 *
 * A fact finder is responsible for searching for RDF statements based on a semantic prompt.
 * IQ. This interface defines the core functionality that all
 * fact finders must implement.
 *
 * An implementation of this interface should provide the following capabilities:
 * - Searching for facts: Implementations should allow searching for facts based on
 *   semantic queries provided as input.
 *
 * Implementations of this interface are expected to return a Model containing the
 * results of the search, representing the facts found based on the semantic query.
 **/
public interface I_ModelFinder extends I_Find<Model> {

    /**
     * Searches for facts (RDF statements) based a semantic query.
     *
     * @param semantic_prompt the semantic query used to search for facts.
     * @return a Model containing the result graph of the search.
     */
    Model find(String semantic_prompt);
}
