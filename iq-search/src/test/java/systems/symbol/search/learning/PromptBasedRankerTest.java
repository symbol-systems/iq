package systems.symbol.search.learning;

import systems.symbol.search.SearchHit;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PromptBasedRankerTest {
    
    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private MockLLMClient mockClient;
    private PromptBasedRanker ranker;
    
    @BeforeEach
    void setUp() {
        mockClient = new MockLLMClient();
        ranker = new PromptBasedRanker(mockClient, 100);
    }
    
    @Test
    void testScoreSingleResult() {
        mockClient.setResponse("0.85");
        
        IRI concept = vf.createIRI("http://example.com/Service");
        SearchHit hit = new SearchHit(
            vf.createIRI("http://example.com/Azure"),
            0.75,
            "Azure database service",
            new HashMap<>()
        );
        
        double score = ranker.scoreResult("database", concept, hit);
        
        assertEquals(0.85, score);
    }
    
    @Test
    void testScoreMultipleResults() {
        mockClient.setResponses(Arrays.asList("0.9", "0.6", "0.4"));
        
        IRI concept = vf.createIRI("http://example.com/Service");
        List<SearchHit> hits = Arrays.asList(
            new SearchHit(vf.createIRI("http://example.com/1"), 0.75, "text1", new HashMap<>()),
            new SearchHit(vf.createIRI("http://example.com/2"), 0.70, "text2", new HashMap<>()),
            new SearchHit(vf.createIRI("http://example.com/3"), 0.65, "text3", new HashMap<>())
        );
        
        List<Double> scores = ranker.scoreResults("database", concept, hits);
        
        assertEquals(3, scores.size());
        assertEquals(0.9, scores.get(0));
        assertEquals(0.6, scores.get(1));
        assertEquals(0.4, scores.get(2));
    }
    
    @Test
    void testScoreExtraction() {
        // Test various response formats
        mockClient.setResponse("0.95");
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        double score = ranker.scoreResult("q", concept, hit);
        assertEquals(0.95, score);
    }
    
    @Test
    void testScoreNormalization() {
        mockClient.setResponse("1.0");  // Perfect relevance
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        double score = ranker.scoreResult("q", concept, hit);
        
        assertTrue(score >= 0.0 && score <= 1.0);
    }
    
    @Test
    void testCaching() {
        mockClient.setResponse("0.8");
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        // First call
        double score1 = ranker.scoreResult("query", concept, hit);
        
        // Change response for second call
        mockClient.setResponse("0.5");
        
        // Second call should return cached value
        double score2 = ranker.scoreResult("query", concept, hit);
        
        assertEquals(score1, score2);  // Cached
        assertEquals(0.8, score1);
    }
    
    @Test
    void testCacheClear() {
        mockClient.setResponse("0.8");
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        double score1 = ranker.scoreResult("query", concept, hit);
        
        // Clear cache
        ranker.clearCache();
        
        // Change response
        mockClient.setResponse("0.5");
        
        // Should return new value
        double score2 = ranker.scoreResult("query", concept, hit);
        
        assertEquals(0.8, score1);
        assertEquals(0.5, score2);
    }
    
    @Test
    void testExplanation() {
        mockClient.setResponse("This result is relevant because it matches the query");
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        String explanation = ranker.explainScore("query", concept, hit);
        
        assertNotNull(explanation);
        assertFalse(explanation.isEmpty());
    }
    
    @Test
    void testGetType() {
        assertEquals("prompt-based", ranker.getType());
    }
    
    @Test
    void testFallbackOnError() {
        mockClient.setThrowError(true);
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.75, "text", new HashMap<>());
        
        // Should not throw, should fallback
        double score = ranker.scoreResult("query", concept, hit);
        
        // Returns original score on error
        assertEquals(0.75, score);
    }
    
    @Test
    void testInvalidScoreHandling() {
        // Response with no valid score
        mockClient.setResponse("The result is very relevant");
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        double score = ranker.scoreResult("query", concept, hit);
        
        // Should default to 0.5 when no score found
        assertEquals(0.5, score);
    }
    
    @Test
    void testScoreBounds() {
        // Score > 1.0 should still be accepted but clamped in system
        mockClient.setResponse("1.5");
        
        IRI concept = vf.createIRI("http://example.com/C");
        SearchHit hit = new SearchHit(vf.createIRI("http://example.com/E"), 0.5, "text", new HashMap<>());
        
        double score = ranker.scoreResult("query", concept, hit);
        
        // LLM Ranker doesn't clamp, that's done at L2R level
        assertEquals(1.5, score);
    }
    
    @Test
    void testAsyncScoring() throws Exception {
        mockClient.setResponses(Arrays.asList("0.8", "0.6"));
        
        IRI concept = vf.createIRI("http://example.com/C");
        List<SearchHit> hits = Arrays.asList(
            new SearchHit(vf.createIRI("http://example.com/1"), 0.5, "t1", new HashMap<>()),
            new SearchHit(vf.createIRI("http://example.com/2"), 0.5, "t2", new HashMap<>())
        );
        
        var future = ranker.scoreResultsAsync("query", concept, hits);
        var scores = future.get();
        
        assertEquals(2, scores.size());
        assertEquals(0.8, scores.get(0));
        assertEquals(0.6, scores.get(1));
    }
    
    // Mock LLM Client
    private static class MockLLMClient implements PromptBasedRanker.LLMClient {
        private String response = "0.5";
        private Queue<String> responses = new LinkedList<>();
        private boolean throwError = false;
        
        void setResponse(String response) {
            this.response = response;
            this.responses.clear();
        }
        
        void setResponses(List<String> responses) {
            this.responses = new LinkedList<>(responses);
        }
        
        void setThrowError(boolean error) {
            this.throwError = error;
        }
        
        @Override
        public String generate(String prompt) throws Exception {
            if (throwError) {
                throw new Exception("Mock LLM error");
            }
            
            if (!responses.isEmpty()) {
                return responses.poll();
            }
            
            return response;
        }
    }
}
