package systems.symbol.lake.ingest;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

//    mvn test -Dtest="IngestTikaTest"

public class IngestTikaTest {
    File from = new File("./src/test/resources/docs/");
    File to = new File("./tested/tika/");

    @Test
    void testIngest() throws RepositoryException, IOException {
        try (FileSystemManager vfs = VFS.getManager()) {
            FileObject fileObject = vfs.resolveFile(new File(from, "test.txt").toURI());
            TikaDocumentIngestor tika = new TikaDocumentIngestor();
            Object content = tika.convert(fileObject).getContent();
            assert content != null;
            assert content.toString().contains("Hello");
        }
    }
}
