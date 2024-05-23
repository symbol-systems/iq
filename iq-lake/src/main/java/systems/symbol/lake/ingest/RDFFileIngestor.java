package systems.symbol.lake.ingest;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.rdf4j.io.RDFLoader;

import java.io.IOException;
import java.util.function.Consumer;

public class RDFFileIngestor implements Consumer<FileObject> {
    private static final Logger log = LoggerFactory.getLogger(RDFFileIngestor.class);
    FileSystemManager vfs;
    RepositoryConnection conn;
    IRI self;
    RDFLoader loader;

    public RDFFileIngestor(RepositoryConnection conn, IRI self) throws FileSystemException {
        // this.ns=ns;
        this.vfs = VFS.getManager();
        this.conn = conn;
        this.self = self;
        loader = new RDFLoader(conn);
    }

    public void accept(FileObject file) {
        RDFFormat format = Rio.getWriterFormatForFileName(file.getName().getBaseName()).orElse(null);
        if (format==null) return;

        IRI asset = Values.iri(file.getURI().toString());
        try {
            loader.load(asset, file.getContent().getInputStream(), format);
            log.info("loaded.rdf: " + format+" @ "+asset);
        } catch (IOException e) {
            log.error("oops: " + e.getMessage()+" @ "+file.getName().getURI(), e);
        }
    }    

}
