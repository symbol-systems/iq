package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import java.io.File;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilesTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();
    private static final IRI baseIRI = vf.createIRI("http://example.org/ns/");

    @Test
    void toIRI_ValidFile_ReturnsIRI() {
        File parentFile = new File("/path/to/home");
        File file = new File("/path/to/home/resource/file.txt");

        IRI result = Files.toIRI(vf, baseIRI, parentFile, file);
        assert result != null;

        assertEquals("http://example.org/ns/resource/file", result.stringValue());
    }

    @Test
    void toIRI_InvalidFile_ReturnsIRI() {
        File parentFile = new File("/path/to/home");
        File file = new File("/other/path/resource/file.txt");

        IRI result = Files.toIRI(vf, baseIRI, parentFile, file);
        assertNull(result);
    }
}
