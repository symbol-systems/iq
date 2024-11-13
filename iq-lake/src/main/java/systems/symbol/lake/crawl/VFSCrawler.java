package systems.symbol.lake.crawl;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.function.Consumer;

public class VFSCrawler implements I_Crawler<FileObject> {
private static final Logger log = LoggerFactory.getLogger(VFSCrawler.class);
FileSystemManager vfs;
FileSystemOptions opts = new FileSystemOptions();
Consumer<FileObject> processor;

public VFSCrawler(Consumer<FileObject> processor) throws FileSystemException {
this.vfs = new VFS();
log.info("VFS.init: {}", processor);
this.processor = processor;
}

public VFSCrawler(DefaultFileSystemManager vfs, Consumer<FileObject> processor) throws FileSystemException {
this.vfs = vfs;
log.info("VFS.init: {} -> {}", vfs.getBaseUri(), processor);
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
log.error("vfs.crawl.failed: " + from, e);
return null;
}
}
}
