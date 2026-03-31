package systems.symbol.search.learning;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class RankingFeedbackTest {
    
    private final ValueFactory vf = SimpleValueFactory.getInstance();
    
    @Test
    void testFeedbackBuilder() {
        IRI concept = vf.createIRI("http://example.com/Service");
        IRI entity = vf.createIRI("http://example.com/Azure");
        
        RankingFeedback feedback = RankingFeedback.builder()
            .query("database")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(5)
            .position(0)
            .userSessionId("user-123")
            .initialScore(0.85)
            .build();
        
        assertEquals("database", feedback.getQuery());
        assertEquals(concept, feedback.getConcept());
        assertEquals(entity, feedback.getResultEntity());
        assertEquals(5, feedback.getRelevanceScore());
        assertEquals(0, feedback.getPosition());
        assertEquals("user-123", feedback.getUserSessionId());
        assertEquals(0.85, feedback.getInitialScore());
    }
    
    @Test
    void testNormalizedRelevance() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity = vf.createIRI("http://example.com/Entity");
        
        RankingFeedback irrelevant = RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(0)
            .build();
        assertEquals(0.0, irrelevant.getNormalizedRelevance());
        
        RankingFeedback neutral = RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(3)
            .build();
        assertEquals(0.5, neutral.getNormalizedRelevance());
        
        RankingFeedback relevant = RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(5)
            .build();
        assertEquals(1.0, relevant.getNormalizedRelevance());
    }
    
    @Test
    void testRelevanceScoreBounds() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity = vf.createIRI("http://example.com/Entity");
        
        // Valid range: 0-5
        assertDoesNotThrow(() -> RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(0)
            .build());
        
        assertDoesNotThrow(() -> RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(5)
            .build());
        
        // Invalid: negative
        assertThrows(IllegalArgumentException.class, () -> 
            RankingFeedback.builder()
                .query("q")
                .concept(concept)
                .resultEntity(entity)
                .relevanceScore(-1)
                .build()
        );
        
        // Invalid: > 5
        assertThrows(IllegalArgumentException.class, () -> 
            RankingFeedback.builder()
                .query("q")
                .concept(concept)
                .resultEntity(entity)
                .relevanceScore(6)
                .build()
        );
    }
    
    @Test
    void testPairwiseLoss() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity1 = vf.createIRI("http://example.com/Entity1");
        IRI entity2 = vf.createIRI("http://example.com/Entity2");
        
        // High relevance at position 0, low relevance at position 1
        RankingFeedback good = RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity1)
            .relevanceScore(5)
            .position(0)
            .build();
        
        RankingFeedback bad = RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity2)
            .relevanceScore(1)
            .position(1)
            .build();
        
        // Loss should be positive (inversion penalty)
        double loss = good.pairwiseLoss(bad);
        assertTrue(loss > 0.0);
    }
    
    @Test
    void testDefaultValues() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity = vf.createIRI("http://example.com/Entity");
        
        RankingFeedback feedback = RankingFeedback.builder()
            .query("test")
            .concept(concept)
            .resultEntity(entity)
            .build();
        
        assertEquals(3, feedback.getRelevanceScore());  // Default: somewhat relevant
        assertEquals(0, feedback.getPosition());
        assertEquals(0.5, feedback.getInitialScore());
        assertNotNull(feedback.getTimestamp());
    }
    
    @Test
    void testTimestamp() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity = vf.createIRI("http://example.com/Entity");
        
        Instant before = Instant.now();
        RankingFeedback feedback = RankingFeedback.builder()
            .query("q")
            .concept(concept)
            .resultEntity(entity)
            .build();
        Instant after = Instant.now();
        
        assertTrue(!feedback.getTimestamp().isBefore(before));
        assertTrue(!feedback.getTimestamp().isAfter(after.plusSeconds(1)));
    }
    
    @Test
    void testToString() {
        IRI concept = vf.createIRI("http://example.com/Concept");
        IRI entity = vf.createIRI("http://example.com/Entity");
        
        RankingFeedback feedback = RankingFeedback.builder()
            .query("test query")
            .concept(concept)
            .resultEntity(entity)
            .relevanceScore(4)
            .build();
        
        String str = feedback.toString();
        assertNotNull(str);
        assertTrue(str.contains("test query"));
        assertTrue(str.contains("4"));
    }
}
