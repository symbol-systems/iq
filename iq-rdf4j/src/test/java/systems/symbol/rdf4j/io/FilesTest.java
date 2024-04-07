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
private static final File home = new File("/path/to/home");

@Test
void toFile_ValidIRI_ReturnsFile() {
IRI ns = vf.createIRI("http://example.org/ns/");
IRI iri = vf.createIRI("http://example.org/ns/resource");

File result = Files.toFile(home, ns, iri);

assert ( null != result);
assertEquals(new File(home, "resource"), result);
}

@Test
void toFile_InvalidIRI_ReturnsNull() {
IRI ns = vf.createIRI("http://example.org/ns/");
IRI iri = vf.createIRI("http://other.org/ns/resource");

File result = Files.toFile(home, ns, iri);

assertNull(result);
}

@Test
void toIRI_ValidFile_ReturnsIRI() {
File parentFile = new File("/path/to/home");
File file = new File("/path/to/home/resource/file.txt");

IRI result = Files.toIRI(vf, baseIRI, parentFile, file);

assertEquals("http://example.org/ns/resource/file.txt", result.stringValue());
}

@Test
void toIRI_InvalidFile_ReturnsIRI() {
File parentFile = new File("/path/to/home");
File file = new File("/other/path/resource/file.txt");

IRI result = Files.toIRI(vf, baseIRI, parentFile, file);
assertNull(result);
}
}
