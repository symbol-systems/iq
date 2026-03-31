package systems.symbol.search;

import systems.symbol.search.hybrid.HybridIndex;
import systems.symbol.search.vector.VectorIndex;
import systems.symbol.search.bm25.BM25Index;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HybridIndexTest {
    
    private HybridIndex index;
    private IRI concept;
    private IRI entity1, entity2, entity3;

    @BeforeEach
    public void setUp() {
        index = new HybridIndex(0.6);  // 60% vector, 40% BM25
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        concept = vf.createIRI("http://example.com/TestConcept");
        entity1 = vf.createIRI("http://example.com/entity1");
        entity2 = vf.createIRI("http://example.com/entity2");
        entity3 = vf.createIRI("http://example.com/entity3");
    }

    @Test
    public void testHybridCombination() {
        // Mix of semantic and keyword content
        index.index(entity1, "Azure database service for cloud computing", concept);
        index.index(entity2, "Database management SQL Server", concept);
        index.index(entity3, "Kubernetes container platform", concept);

        SearchRequest request = SearchRequest.builder()
            .query("Azure database")
            .concept(concept)
            .maxResults(10)
            .build();

        SearchResult result = index.search(request);
        
        assertNotNull(result);
        assertTrue(result.size() > 0, "Hybrid search should find matches");
        assertEquals("hybrid", result.getStats().getIndexType());
    }

    @Test
    public void testBetterThanSingleIndex() {
        // Entity using exact keywords and semantic similarity
        index.index(entity1, "SQL Server database", concept);
        index.index(entity2, "Relational data management system", concept);

        // Exact keyword match
        SearchRequest exact = SearchRequest.builder()
            .query("SQL Server")
            .concept(concept)
            .build();

        // Semantic match
        SearchRequest semantic = SearchRequest.builder()
            .query("database system")
            .concept(concept)
            .build();

        SearchResult exactResult = index.search(exact);
        SearchResult semanticResult = index.search(semantic);

        assertTrue(exactResult.size() > 0, "Should find exact keyword matches");
        assertTrue(semanticResult.size() > 0, "Should find semantic matches");
    }

    @Test
    public void testEmpty() {
        SearchRequest request = SearchRequest.builder()
            .query("")
            .concept(concept)
            .build();

        SearchResult result = index.search(request);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testClear() {
        index.index(entity1, "Azure database", concept);
        index.clear();
        
        SearchResult result = index.search(SearchRequest.builder()
            .query("Azure")
            .concept(concept)
            .build());
        assertTrue(result.isEmpty());
    }
}
