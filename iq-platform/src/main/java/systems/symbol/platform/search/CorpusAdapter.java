package systems.symbol.platform.search;

import systems.symbol.search.*;
import systems.symbol.finder.I_Corpus;
import systems.symbol.finder.I_Found;
import systems.symbol.finder.I_Search;
import org.eclipse.rdf4j.model.IRI;
import java.util.*;

/**
 * Adapter to use new I_Index search infrastructure through legacy I_Corpus interface.
 * 
 * Bridges between new pluggable search (I_Index) and existing code expecting I_Corpus.
 * Enables gradual migration without refactoring all consumers.
 * 
 * Usage:
 * <pre>
 * I_Index newIndex = IndexFactory.hybrid(0.6);
 * I_Corpus<IRI> legacyCorpus = new CorpusAdapter(newIndex);
 * // Now can use with existing code that expects I_Corpus
 * I_Search<I_Found<IRI>> search = legacyCorpus.byConcept(concept);
 * Collection<I_Found<IRI>> results = search.search(query, maxResults, minScore);
 * </pre>
 */
public class CorpusAdapter implements I_Corpus<IRI> {
    private final I_Index index;
    
    public CorpusAdapter(I_Index index) {
        this.index = index;
    }
    
    @Override
    public I_Search<I_Found<IRI>> byConcept(IRI concept) {
        return (query, maxResults, minScore) -> {
            // Delegate to new index API
            SearchResult result = index.search(SearchRequest.builder()
                .query(query != null ? query : "")
                .concept(concept)
                .maxResults(maxResults)
                .minScore(minScore)
                .build());
            
            // Convert SearchHit to I_Found<IRI>
            return new ArrayList<>(result.getHits());
        };
    }
    
    /**
     * Direct access to wrapped I_Index for new code.
     */
    public I_Index getIndex() {
        return index;
    }
}
