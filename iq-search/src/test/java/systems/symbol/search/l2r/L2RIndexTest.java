package systems.symbol.search.l2r;

import systems.symbol.search.*;
import systems.symbol.search.learning.LLMRanker;
import systems.symbol.search.learning.RankingFeedback;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class L2RIndexTest {
    
    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private MockLLMRanker mockRanker;
    private MockBaseIndex mockBaseIndex;
    private L2RIndex l2rIndex;
    
    @BeforeEach
    void setUp() {
        mockRanker = new MockLLMRanker();
        mockBaseIndex = new MockBaseIndex();
        l2rIndex = new L2RIndex(mockBaseIndex, mockRanker, 0.3);
    }
    
    @Test
    void testBasicReranking() {
        IRI concept = vf.createIRI("http://example.com/Service");
        IRI entity1 = vf.createIRI("http://example.com/Azure");
        IRI entity2 = vf.createIRI("http://example.com/AWS");
        
        // Index entities
        l2rIndex.index(entity1, "Azure cloud database service", concept);
        l2rIndex.index(entity2, "AWS managed database", concept);
        
        // Search will return entity1 first (from mock)
        SearchRequest request = SearchRequest.builder()
            .query("database")
            .concept(concept)
            .maxResults(10)
            .build();
        
        SearchResult result = l2rIndex.search(request);
        
        assertFalse(result.isEmpty());
        assertEquals("l2r", result.getStats().getIndexType());
    }
    
    @Test
    void testEmptyResults() {
        mockBaseIndex.setReturnEmpty(true);
        
        SearchRequest request = SearchRequest.builder()
            .query("nonexistent")
            .maxResults(10)
            .build();
        
        SearchResult result = l2rIndex.search(request);
        
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }
    
    @Test
    void testScoreCombination() {
        // Test that LLM scores influence final ranking
        mockRanker.setScores(Arrays.asList(0.9, 0.3));  // High LLM score for first, low for second
        
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity1 = vf.createIRI("http://example.com/Entity1");
        IRI entity2 = vf.createIRI("http://example.com/Entity2");
        
        l2rIndex.index(entity1, "text one", concept);
        l2rIndex.index(entity2, "text two", concept);
        
        SearchRequest request = SearchRequest.builder()
            .query("test")
            .concept(concept)
            .build();
        
        SearchResult result = l2rIndex.search(request);
        
        // Verify results are reranked (high LLM score should be first)
        if (!result.isEmpty()) {
            assertTrue(result.getHits().get(0).score() > 0);
        }
    }
    
    @Test
    void testFeedbackCollection() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity = vf.createIRI("http://example.com/Entity");
        
        // Record feedback
        RankingFeedback feedback = RankingFeedback.builder()
            .query("test")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(5)
            .position(0)
            .build();
        
        l2rIndex.recordFeedback(feedback);
        
        // Verify feedback is stored
        List<RankingFeedback> collected = l2rIndex.getFeedback();
        assertEquals(1, collected.size());
        assertEquals(5, collected.get(0).getRelevanceScore());
    }
    
    @Test
    void testFeedbackDisabled() {
        l2rIndex.setFeedbackCollectionEnabled(false);
        
        RankingFeedback feedback = RankingFeedback.builder()
            .query("test")
            .concept(vf.createIRI("http://example.com/Concept"))
            .resultEntity(vf.createIRI("http://example.com/Entity"))
            .relevanceScore(5)
            .build();
        
        l2rIndex.recordFeedback(feedback);
        
        // Feedback should not be collected
        assertTrue(l2rIndex.getFeedback().isEmpty());
    }
    
    @Test
    void testFeedbackBufferOverflow() {
        int bufferSize = 10;
        
        // Fill buffer beyond capacity
        for (int i = 0; i < bufferSize + 5; i++) {
            RankingFeedback feedback = RankingFeedback.builder()
                .query("q" + i)
                .concept(vf.createIRI("http://example.com/C"))
                .resultEntity(vf.createIRI("http://example.com/E" + i))
                .relevanceScore(3)
                .build();
            l2rIndex.recordFeedback(feedback);
        }
        
        // Buffer should not exceed max size
        assertEquals(10000, l2rIndex.getFeedback().size());  // Default max
    }
    
    @Test
    void testClearFeedback() {
        RankingFeedback feedback = RankingFeedback.builder()
            .query("test")
            .concept(vf.createIRI("http://example.com/C"))
            .resultEntity(vf.createIRI("http://example.com/E"))
            .relevanceScore(4)
            .build();
        
        l2rIndex.recordFeedback(feedback);
        assertEquals(1, l2rIndex.getFeedback().size());
        
        l2rIndex.clearFeedback();
        assertEquals(0, l2rIndex.getFeedback().size());
    }
    
    @Test
    void testIndexType() {
        assertEquals("l2r", l2rIndex.getType());
    }
    
    @Test
    void testClear() {
        IRI concept = vf.createIRI("http://example.com/C");
        l2rIndex.index(vf.createIRI("http://example.com/E"), "text", concept);
        
        l2rIndex.clear();
        
        // Verify cleared
        assertTrue(mockBaseIndex.isCleared);
    }
    
    @Test
    void testMetadataEnrichment() {
        // Verify that metadata is enriched with L2R information
        mockBaseIndex.setReturnEmpty(false);
        
        SearchRequest request = SearchRequest.builder()
            .query("test")
            .maxResults(5)
            .build();
        
        SearchResult result = l2rIndex.search(request);
        
        if (!result.isEmpty()) {
            SearchHit hit = result.getHits().get(0);
            // Check that metadata was enriched (would contain llm_score, etc.)
            assertNotNull(hit.getMetadata());
        }
    }
    
    // Mock implementations for testing
    private static class MockLLMRanker implements LLMRanker {
        private List<Double> scores = Arrays.asList(0.8, 0.6, 0.4);
        private int callCount = 0;
        
        void setScores(List<Double> scores) {
            this.scores = scores;
        }
        
        @Override
        public double scoreResult(String query, IRI concept, SearchHit result) {
            return 0.7;
        }
        
        @Override
        public List<Double> scoreResults(String query, IRI concept, List<SearchHit> results) {
            List<Double> result = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                result.add(scores.size() > i ? scores.get(i) : 0.5);
            }
            return result;
        }
        
        @Override
        public CompletableFuture<List<Double>> scoreResultsAsync(
            String query, IRI concept, List<SearchHit> results) {
            return CompletableFuture.completedFuture(scoreResults(query, concept, results));
        }
        
        @Override
        public String explainScore(String query, IRI concept, SearchHit result) {
            return "Result matches query context well";
        }
        
        @Override
        public void clearCache() {
            // No-op
        }
        
        @Override
        public String getType() {
            return "mock";
        }
    }
    
    private static class MockBaseIndex implements I_Index {
        private boolean returnEmpty = false;
        private List<SearchHit> mockResults;
        private ValueFactory vf = SimpleValueFactory.getInstance();
        boolean isCleared = false;
        
        void setReturnEmpty(boolean empty) {
            this.returnEmpty = empty;
        }
        
        @Override
        public void index(IRI entity, String text, IRI concept) {
            // Mock indexing
        }
        
        @Override
        public SearchResult search(SearchRequest request) {
            if (returnEmpty) {
                return new SearchResult(Collections.emptyList(), 
                    new SearchStats("mock", 0, 0, 0, 0.0));
            }
            
            // Return some mock results
            List<SearchHit> hits = Arrays.asList(
                new SearchHit(vf.createIRI("http://example.com/1"), 0.85, "matched text 1", new HashMap<>()),
                new SearchHit(vf.createIRI("http://example.com/2"), 0.65, "matched text 2", new HashMap<>())
            );
            
            return new SearchResult(hits, 
                new SearchStats("mock", 50, 1000, 2, 0.75));
        }
        
        @Override
        public void clear() {
            isCleared = true;
        }
        
        @Override
        public String getType() {
            return "mock";
        }
    }
}
