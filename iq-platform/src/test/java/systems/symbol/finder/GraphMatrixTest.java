package systems.symbol.finder;

import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import systems.symbol.lake.Lakes;
import systems.symbol.platform.IQ_NS;

import java.io.File;
import java.io.IOException;

import org.eclipse.rdf4j.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphMatrixTest {

    private SearchMatrix searchMatrix;
    private EmbeddingModel model;
    private Repository repo;
    private File assets = new File("assets/triples");

    @BeforeEach
    public void setUp() throws IOException {
        model = new AllMiniLmL6V2EmbeddingModel();
        searchMatrix = new SearchMatrix(model);
        repo = Lakes.load(assets, IQ_NS.TEST);
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
