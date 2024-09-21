package systems.symbol.finder;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.IQ_NS;

import java.util.Collection;

public class SearchMatrixTest {

    private SearchMatrix searchMatrix;
    private EmbeddingModel model;
    private static final IRI BASE_IRI = Values.iri(IQ_NS.TEST);

    @BeforeMethod
    public void setUp() {
        model = new AllMiniLmL6V2EmbeddingModel();
        searchMatrix = new SearchMatrix(model);
    }

    @Test
    public void testReindex() {
        IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
        IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
        String content = "Sample content";

        searchMatrix.reindex(entity, content, concept);

        Assert.assertNotNull(searchMatrix.byConcept(concept));
        Assert.assertTrue(searchMatrix.indexed(entity));
    }

    @Test
    public void testSearchPositive() throws StateException {
        IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
        IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
        String content = "Sample content";
        String query = "Sample content"; // Query matches the indexed content

        searchMatrix.reindex(entity, content, concept);

        Collection<I_Found<IRI>> results = searchMatrix.search(query, 10, 0.5);

        Assert.assertNotNull(results);
        Assert.assertEquals(results.size(), 1); // Should find exactly one result
        I_Found<IRI> found = results.iterator().next();
        Assert.assertNotNull(found);
        System.out.println("search.matrix.found:" + found.intent() + " -> " + found.score());
        Assert.assertEquals(found.intent(), entity);
    }

    @Test
    public void testSearchNegative() {
        IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
        IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
        String content = "Sample content";
        String query = "Different query"; // Query does not match the indexed content

        // Manually create embeddings
        float[] mockVector = new float[] { 0.1f, 0.2f, 0.3f };
        Embedding embedding = new Embedding(mockVector);
        Response<Embedding> queryResponse = new Response<>(embedding);

        assert queryResponse.content().vector().length == embedding.vector().length;
        searchMatrix.reindex(entity, content, concept);

        Collection<I_Found<IRI>> results = searchMatrix.search(query, 10, 0.5);

        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void testSearchWithNoResults() {
        String query = "Nonexistent content";
        Collection<I_Found<IRI>> results = searchMatrix.search(query, 10, 0.5);

        Assert.assertNotNull(results);
        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void testSearchWithLowScoreThreshold() throws StateException {
        IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
        IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
        String content = "Sample content";
        String query = "Sample content";

        searchMatrix.reindex(entity, content, concept);

        Collection<I_Found<IRI>> results = searchMatrix.search(query, 10, 0.0);

        Assert.assertNotNull(results);
        Assert.assertEquals(results.size(), 1);
        System.out.println("search.matrix.score-low:" + results);
        I_Found<IRI> found = results.iterator().next();
        Assert.assertNotNull(found);
        System.out.println("search.matrix.found:" + found.intent() + " -> " + found.score());
        Assert.assertEquals(found.intent(), entity);
    }
}
