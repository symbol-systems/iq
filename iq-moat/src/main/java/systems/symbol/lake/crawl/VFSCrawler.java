package systems.symbol.lake.crawl;

import com.github.vfss3.S3FileProvider;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.cache.DefaultFilesCache;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.http.HttpFileProvider;
import org.apache.commons.vfs2.provider.https.HttpsFileProvider;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.function.Consumer;

public class VFSCrawler implements I_Crawler<FileObject> {
    private static final Logger log = LoggerFactory.getLogger(VFSCrawler.class);
    DefaultFileSystemManager vfs;
    FileSystemOptions opts = new FileSystemOptions();
    Consumer<FileObject> processor;

    public VFSCrawler(Consumer<FileObject> processor) throws FileSystemException {
        this.vfs = new VFS();
        log.info("VFS.init: {}", processor);
        this.processor = processor;
    }

    public IRI crawl(URI from) {
        return crawl(from, processor);
    }

    @Override
    public IRI crawl(URI from, Consumer<FileObject> next) {
        try {
            FileObject source = vfs.resolveFile(from);
            String sourceURI = source.getPublicURIString();
            log.debug("vfs.crawl: {} --> {}", from, sourceURI);
            Walkers.recurse(this.vfs, opts, sourceURI, next);
            return SimpleValueFactory.getInstance().createIRI(sourceURI);
        } catch (FileSystemException e) {
            log.error("vfs.crawl.failed: "+from, e);
            return null;
        }
    }
}
