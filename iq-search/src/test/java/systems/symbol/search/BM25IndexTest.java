package systems.symbol.search;

import systems.symbol.search.bm25.BM25Index;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BM25IndexTest {
    
    private BM25Index index;
    private IRI concept;
    private IRI entity1, entity2, entity3;

    @BeforeEach
    public void setUp() {
        index = new BM25Index();
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        concept = vf.createIRI("http://example.com/TestConcept");
        entity1 = vf.createIRI("http://example.com/entity1");
        entity2 = vf.createIRI("http://example.com/entity2");
        entity3 = vf.createIRI("http://example.com/entity3");
    }

    @Test
    public void testBM25KeywordMatching() {
        index.index(entity1, "Azure SQL database management system", concept);
        index.index(entity2, "PostgreSQL open source database", concept);
        index.index(entity3, "MongoDB NoSQL solution", concept);

        // Exact keyword match should score well
        SearchRequest request = SearchRequest.builder()
            .query("SQL database")
            .concept(concept)
            .maxResults(10)
            .build();

        SearchResult result = index.search(request);
        
        assertFalse(result.isEmpty(), "Should find matches for 'SQL database'");
        assertTrue(result.getHits().get(0).intent().equals(entity1) ||
                   result.getHits().get(0).intent().equals(entity2),
                   "entity1 or entity2 should rank highest");
    }

    @Test
    public void testBM25TermFrequency() {
        // entity1 has "database" 3 times
        index.index(entity1, "database database database management", concept);
        // entity2 has "database" once
        index.index(entity2, "database solution architecture", concept);

        SearchRequest request = SearchRequest.builder()
            .query("database")
            .concept(concept)
            .build();

        SearchResult result = index.search(request);
        
        // entity1 should rank higher due to term frequency
        if (result.size() >= 2) {
            assertTrue(result.getHits().get(0).intent().equals(entity1),
                      "entity1 with higher term frequency should rank first");
        }
    }

    @Test
    public void testEmptyIndex() {
        SearchRequest request = SearchRequest.builder()
            .query("anything")
            .build();

        SearchResult result = index.search(request);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConceptFilter() {
        IRI concept2 = SimpleValueFactory.getInstance()
            .createIRI("http://example.com/OtherConcept");

        index.index(entity1, "Azure database", concept);
        index.index(entity2, "SQL server", concept2);

        SearchRequest request = SearchRequest.builder()
            .query("database")
            .concept(concept2)
            .build();

        SearchResult result = index.search(request);
        // entity1 indexed under concept, searching only concept2
        // Should return nothing or only entity2
        for (SearchHit hit : result.getHits()) {
            assertNotEquals(entity1, hit.intent());
        }
    }

    @Test
    public void testClear() {
        index.index(entity1, "Azure database", concept);
        assertFalse(index.search(SearchRequest.builder().query("Azure").build()).isEmpty());

        index.clear();
        assertTrue(index.search(SearchRequest.builder().query("Azure").build()).isEmpty());
    }
}
