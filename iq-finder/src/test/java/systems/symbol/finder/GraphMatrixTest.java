package systems.symbol.finder;

import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.BootstrapRepository;

import java.io.File;
import java.io.IOException;

public class GraphMatrixTest {

    private SearchMatrix searchMatrix;
    private EmbeddingModel model;
    private BootstrapRepository repo;
    private File assets = new File("assets/triples");

    @BeforeMethod
    public void setUp() throws IOException {
        model = new AllMiniLmL6V2EmbeddingModel();
        searchMatrix = new SearchMatrix(model);
        repo = new BootstrapRepository(assets, IQ_NS.TEST);
    }

    @Test
    public void testIndex() {
        // IRI concept = Values.iri(BASE_IRI.stringValue() + "concept");
        // IRI entity = Values.iri(BASE_IRI.stringValue() + "entity");
        // String content = "Sample content";

        // searchMatrix.reindex(entity, content, concept);

        // Assert.assertNotNull(searchMatrix.byConcept(concept));
        // Assert.assertTrue(searchMatrix.indexed(entity));
    }
}
