package systems.symbol.platform.search;

import systems.symbol.search.*;
import systems.symbol.finder.I_Corpus;
import systems.symbol.finder.I_Found;
import systems.symbol.finder.I_Search;
import systems.symbol.llm.gpt.LLMFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for L2R with iq-platform.
 * Tests the complete pipeline: iq-search with iq-platform integrations.
 */
class SearchIntegrationTest {
    
    private final SimpleValueFactory vf = SimpleValueFactory.getInstance();
    private IRI concept;
    private IRI entity1, entity2, entity3;
    
    @BeforeEach
    void setUp() {
        concept = vf.createIRI("http://example.com/Service");
        entity1 = vf.createIRI("http://example.com/AzureDB");
        entity2 = vf.createIRI("http://example.com/AWSDB");
        entity3 = vf.createIRI("http://example.com/Database");
    }
    
    @Test
    void testHybridSearchWithCorpusAdapter() {
        // Create hybrid search
        I_Index hybridIndex = IndexFactory.hybrid(0.6);
        
        // Index entities
        hybridIndex.index(entity1, "Azure SQL Database service", concept);
        hybridIndex.index(entity2, "AWS RDS database solution", concept);
        hybridIndex.index(entity3, "Generic database management", concept);
        
        // Adapt to legacy I_Corpus interface
        I_Corpus<IRI> corpus = new CorpusAdapter(hybridIndex);
        
        // Use legacy API
        I_Search<I_Found<IRI>> search = corpus.byConcept(concept);
        assertNotNull(search);
        
        Collection<I_Found<IRI>> results = search.search("Azure database", 10, 0.3);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        
        // Verify entity1 is in results
        boolean found = results.stream()
            .anyMatch(r -> r.intent().equals(entity1));
        assertTrue(found, "Azure entity should be in results");
    }
    
    @Test
    void testCorpusAdapterEmptyQuery() {
        I_Index index = IndexFactory.hybrid(0.6);
        index.index(entity1, "text", concept);
        
        I_Corpus<IRI> corpus = new CorpusAdapter(index);
        I_Search<I_Found<IRI>> search = corpus.byConcept(concept);
        
        Collection<I_Found<IRI>> results = search.search(null, 10, 0.3);
        assertTrue(results.isEmpty(), "Empty query should return no results");
    }
    
    @Test
    void testCorpusAdapterConceptFiltering() {
        I_Index index = IndexFactory.hybrid(0.6);
        
        IRI concept2 = vf.createIRI("http://example.com/OtherConcept");
        index.index(entity1, "Cloud database", concept);
        index.index(entity2, "On-premise database", concept2);
        
        I_Corpus<IRI> corpus = new CorpusAdapter(index);
        
        // Search only in concept
        I_Search<I_Found<IRI>> search = corpus.byConcept(concept);
        Collection<I_Found<IRI>> results = search.search("database", 10, 0.0);
        
        // Should find entity1 but not entity2
        for (I_Found<IRI> hit : results) {
            assertNotEquals(entity2, hit.intent());
        }
    }
    
    @Test
    void testFactoryCreation() {
        // Test factory methods without GPTWrapper (mock LLM)
        I_Index vector = IndexFactory.vector();
        assertNotNull(vector);
        assertEquals("vector", vector.getType());
        
        I_Index bm25 = IndexFactory.bm25();
        assertNotNull(bm25);
        assertEquals("bm25", bm25.getType());
        
        I_Index hybrid = IndexFactory.hybrid(0.6);
        assertNotNull(hybrid);
        assertEquals("hybrid", hybrid.getType());
    }
    
    @Test
    void testFactoryFromConfig() {
        I_Index index = IndexFactory.fromConfig("hybrid:0.7");
        assertNotNull(index);
        assertEquals("hybrid", index.getType());
    }
    
    @Test
    void testSearchRequestBuilder() {
        SearchRequest request = SearchRequest.builder()
            .query("test")
            .concept(concept)
            .maxResults(20)
            .minScore(0.3)
            .build();
        
        assertEquals("test", request.getQuery());
        assertEquals(concept, request.getConcept());
        assertEquals(20, request.getMaxResults());
        assertEquals(0.3, request.getMinScore());
    }
    
    @Test
    void testSearchHitAndResult() {
        SearchHit hit = new SearchHit(entity1, 0.85, "matched text");
        
        assertEquals(entity1, hit.intent());
        assertEquals(0.85, hit.score());
        assertEquals("matched text", hit.getMatchedText());
        
        SearchResult result = new SearchResult(
            java.util.List.of(hit),
            new SearchStats("test", 100, 1, 50, 0.85)
        );
        
        assertEquals(1, result.size());
        assertFalse(result.isEmpty());
        assertTrue(result.best().isPresent());
        assertEquals(entity1, result.best().get().intent());
    }
    
    @Test
    void testIndexClear() {
        I_Index index = IndexFactory.hybrid(0.6);
        index.index(entity1, "test", concept);
        
        I_Corpus<IRI> corpus = new CorpusAdapter(index);
        I_Search<I_Found<IRI>> search = corpus.byConcept(concept);
        
        // Should find before clear
        Collection<I_Found<IRI>> resultsBefore = search.search("test", 10, 0.0);
        assertTrue(resultsBefore.size() > 0);
        
        // Clear and search again
        index.clear();
        Collection<I_Found<IRI>> resultsAfter = search.search("test", 10, 0.0);
        assertTrue(resultsAfter.isEmpty());
    }
}
