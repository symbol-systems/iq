package systems.symbol.search;

import systems.symbol.search.config.SearchConfiguration;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IndexFactoryTest {
    
    @Test
    public void testVectorIndexCreation() {
        I_Index index = IndexFactory.vector();
        assertNotNull(index);
        assertEquals("vector", index.getType());
    }

    @Test
    public void testBM25IndexCreation() {
        I_Index index = IndexFactory.bm25();
        assertNotNull(index);
        assertEquals("bm25", index.getType());
    }

    @Test
    public void testHybridIndexCreation() {
        I_Index index = IndexFactory.hybrid(0.6);
        assertNotNull(index);
        assertEquals("hybrid", index.getType());
    }

    @Test
    public void testGraphIndexCreation() {
        Model model = new LinkedHashModel();
        I_Index index = IndexFactory.graph(model, 0.3);
        assertNotNull(index);
        assertEquals("graph", index.getType());
    }

    @Test
    public void testFromConfigVector() {
        I_Index index = IndexFactory.fromConfig("vector");
        assertEquals("vector", index.getType());
    }

    @Test
    public void testFromConfigBM25() {
        I_Index index = IndexFactory.fromConfig("bm25");
        assertEquals("bm25", index.getType());
    }

    @Test
    public void testFromConfigHybridWithWeight() {
        I_Index index = IndexFactory.fromConfig("hybrid:0.7");
        assertEquals("hybrid", index.getType());
    }

    @Test
    public void testFromConfigGraphWithBoost() {
        Model model = new LinkedHashModel();
        I_Index index = IndexFactory.fromConfig("graph:0.4", model);
        assertEquals("graph", index.getType());
    }

    @Test
    public void testFromConfigDefault() {
        I_Index index = IndexFactory.fromConfig(null);
        assertEquals("hybrid", index.getType());  // Default
    }

    @Test
    public void testFromConfigInvalid() {
        I_Index index = IndexFactory.fromConfig("invalid");
        assertEquals("hybrid", index.getType());  // Falls back to default
    }

    @Test
    public void testSearchConfigurationBuilder() {
        SearchConfiguration config = SearchConfiguration.builder()
            .indexType(SearchConfiguration.IndexType.HYBRID)
            .vectorWeight(0.7)
            .bm25Weight(0.3)
            .graphSearchEnabled(true)
            .graphBoost(0.4)
            .build();

        assertEquals(SearchConfiguration.IndexType.HYBRID, config.getIndexType());
        assertEquals(0.7, config.getVectorWeight());
        assertEquals(0.4, config.getGraphBoost());
        assertTrue(config.isGraphSearchEnabled());
    }
}
