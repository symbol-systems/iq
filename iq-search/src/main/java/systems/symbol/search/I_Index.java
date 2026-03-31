package systems.symbol.search;

import systems.symbol.finder.I_Found;
import org.eclipse.rdf4j.model.IRI;

import java.util.*;

/**
 * Core search abstraction supporting multiple indexing strategies.
 * Implementations provide vector search, BM25 full-text, hybrid, and graph-based retrieval.
 */
public interface I_Index {
    
    /**
     * Index an entity with text content under a concept.
     */
    void index(IRI entity, String text, IRI concept);
    
    /**
     * Search for ranked results matching the query.
     */
    SearchResult search(SearchRequest request);
    
    /**
     * Clear all indexes.
     */
    void clear();
    
    /**
     * Get index type identifier.
     */
    String getType();
}
