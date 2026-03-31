package systems.symbol.search;

import systems.symbol.search.graph.GraphIndex;
import systems.symbol.search.hybrid.HybridIndex;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GraphIndexTest {
    
    private GraphIndex index;
    private IRI concept;
    private IRI entity1, entity2, entity3;
    private Model model;
    private ValueFactory vf;

    @BeforeEach
    public void setUp() {
        vf = SimpleValueFactory.getInstance();
        model = new LinkedHashModel();
        
        concept = vf.createIRI("http://example.com/Service");
        entity1 = vf.createIRI("http://example.com/AzureDB");
        entity2 = vf.createIRI("http://example.com/Database");
        entity3 = vf.createIRI("http://example.com/Unrelated");

        // Build a simple graph: Service -> Database
        model.add(concept, RDF.TYPE, RDFS.CLASS);
        model.add(entity2, RDF.TYPE, RDFS.CLASS);
        model.add(concept, RDFS.SUBCLASSOF, entity2);  // Service is-a Database
        
        index = new GraphIndex(new HybridIndex(0.6), model, 0.3);
    }

    @Test
    public void testGraphAwareness() {
        index.index(entity1, "Azure database service", concept);
        index.index(entity2, "Generic database", concept);
        index.index(entity3, "Unrelated service", concept);

        SearchRequest request = SearchRequest.builder()
            .query("database service")
            .concept(concept)
            .maxResults(10)
            .build();

        SearchResult result = index.search(request);
        
        assertNotNull(result);
        assertEquals("graph", result.getStats().getIndexType());
    }

    @Test
    public void testProximityBoost() {
        // Add some entities with graph relationships
        IRI parent = vf.createIRI("http://example.com/Cloud");
        IRI child = vf.createIRI("http://example.com/CloudDatabase");
        
        model.add(parent, RDFS.SUBCLASSOF, concept);
        model.add(child, RDFS.SUBCLASSOF, parent);
        
        index.updateModel(model);
        
        index.index(parent, "Cloud computing platform", concept);
        index.index(child, "Cloud database service", concept);

        SearchRequest request = SearchRequest.builder()
            .query("cloud database")
            .concept(concept)
            .build();

        SearchResult result = index.search(request);
        assertTrue(result.size() > 0, "Should find graph-related entities");
    }

    @Test
    public void testEmptyGraph() {
        Model empty = new LinkedHashModel();
        GraphIndex emptyIndex = new GraphIndex(new HybridIndex(0.6), empty, 0.3);
        
        emptyIndex.index(entity1, "database", concept);
        
        SearchResult result = emptyIndex.search(SearchRequest.builder()
            .query("database")
            .concept(concept)
            .build());
        
        assertTrue(result.size() > 0, "Should work with empty graph");
    }

    @Test
    public void testUpdateModel() {
        index.index(entity1, "Azure database", concept);
        
        // Update with new model
        Model newModel = new LinkedHashModel();
        newModel.add(concept, RDFS.LABEL, vf.createLiteral("Service"));
        index.updateModel(newModel);
        
        SearchResult result = index.search(SearchRequest.builder()
            .query("Azure")
            .concept(concept)
            .build());
        
        assertTrue(result.size() > 0);
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
