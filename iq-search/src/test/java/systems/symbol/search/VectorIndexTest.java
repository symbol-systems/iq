package systems.symbol.search;

import systems.symbol.search.vector.VectorIndex;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorIndexTest {
    
    private VectorIndex index;
    private IRI concept;
    private IRI entity1, entity2, entity3;

    @BeforeEach
    public void setUp() {
        index = new VectorIndex();
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        concept = vf.createIRI("http://example.com/TestConcept");
        entity1 = vf.createIRI("http://example.com/entity1");
        entity2 = vf.createIRI("http://example.com/entity2");
        entity3 = vf.createIRI("http://example.com/entity3");
    }

    @Test
    public void testIndexAndSearch() {
        // Index entities
        index.index(entity1, "Azure database service", concept);
        index.index(entity2, "Azure SQL Server pricing", concept);
        index.index(entity3, "AWS EC2 instances", concept);

        // Search for "Azure database"
        SearchRequest request = SearchRequest.builder()
            .query("Azure database")
            .concept(concept)
            .maxResults(10)
            .minScore(0.3)
            .build();

        SearchResult result = index.search(request);

        assertNotNull(result);
        assertTrue(result.getHits().size() > 0, "Should find at least one match");
        assertEquals("vector", result.getStats().getIndexType());
        
        // entity1 should be first (best match)
        assertTrue(result.getHits().get(0).intent().equals(entity1) || 
                   result.getHits().get(0).intent().equals(entity2));
    }

    @Test
    public void testEmptyQuery() {
        index.index(entity1, "Azure database", concept);

        SearchRequest request = SearchRequest.builder()
            .query("")
            .concept(concept)
            .maxResults(10)
            .build();

        SearchResult result = index.search(request);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConceptFiltering() {
        IRI concept2 = SimpleValueFactory.getInstance()
            .createIRI("http://example.com/OtherConcept");

        index.index(entity1, "Azure database", concept);
        index.index(entity2, "Azure SQL Server", concept2);

        // Search only in concept2
        SearchRequest request = SearchRequest.builder()
            .query("Azure")
            .concept(concept2)
            .maxResults(10)
            .build();

        SearchResult result = index.search(request);
        
        // Should find entity2 but not entity1
        for (SearchHit hit : result.getHits()) {
            assertNotEquals(entity1, hit.intent());
        }
    }

    @Test
    public void testMinScoreThreshold() {
        index.index(entity1, "database", concept);
        index.index(entity2, "completely different content", concept);

        SearchRequest request = SearchRequest.builder()
            .query("database")
            .concept(concept)
            .minScore(0.8)  // High threshold
            .maxResults(10)
            .build();

        SearchResult result = index.search(request);
        assertTrue(result.getHits().size() <= 1);
    }

    @Test
    public void testContentDeduplication() {
        // Index same content twice
        index.index(entity1, "Azure database service", concept);
        index.index(entity1, "Azure database service", concept);  // Exact duplicate

        SearchRequest request = SearchRequest.builder()
            .query("Azure database")
            .concept(concept)
            .build();

        SearchResult result = index.search(request);
        assertEquals(1, result.getHits().size(), "Should deduplicate identical content");
    }

    @Test
    public void testClear() {
        index.index(entity1, "Azure database", concept);
        index.index(entity2, "SQL Server", concept);

        // Before clear
        SearchRequest request = SearchRequest.builder()
            .query("Azure")
            .concept(concept)
            .build();
        assertTrue(index.search(request).size() > 0);

        // After clear
        index.clear();
        assertTrue(index.search(request).isEmpty());
    }
}
