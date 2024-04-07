package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.rdf4j.io.FileFormats;
import systems.symbol.rdf4j.io.RDFLoader;
import systems.symbol.rdf4j.io.Remodel;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class TextAsRDFIngestor<T> implements Consumer<FileObject> {
    private static final Logger log = LoggerFactory.getLogger(TextAsRDFIngestor.class);
    FileSystemManager vfs;
    RepositoryConnection conn;
    RDFLoader loader;
    Consumer<ContentEntity<T>> next;

    public TextAsRDFIngestor(RepositoryConnection conn) throws FileSystemException {
        this(conn,null);
    }

    public TextAsRDFIngestor(RepositoryConnection conn, Consumer<ContentEntity<T>> next) throws FileSystemException {
        this.next = next;
        this.vfs = VFS.getManager();
        this.conn = conn;
        this.loader = new RDFLoader( conn);
    }

    public IRI getSupportedMime(String uri) {
        String mime = FileFormats.toSupportedMimetype(uri);
        if (mime == null)
            return null;
        return Remodel.mimetype(mime);
    }

    @Override
    public void accept(FileObject entity) {
            String uri = entity.getURI().toString();

            IRI mime = getSupportedMime(uri);
            if (mime == null) {
                log.warn("not.supported: " + uri);
                return;
            }
            IRI assetIRI = this.conn.getValueFactory().createIRI(uri);
            try {
                Literal content = loader.content(assetIRI, entity.getContent().getInputStream(), mime);
                if (next!=null) next.accept(new ContentEntity<T>(assetIRI,content.stringValue(),null));
                log.info("loaded.txt: " + mime+" @ "+assetIRI);
            } catch (RDFParseException | RepositoryException | IOException e) {
                log.error("oops: " + e.getMessage() + " @ " + uri,e);
            }
    }

}
