package systems.symbol.lake.ingest;

import systems.symbol.string.PrettyString;
import org.apache.commons.vfs2.FileFilterSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class FileCloneIngestor implements Consumer<FileObject> {
private static final Logger log = LoggerFactory.getLogger(FileCloneIngestor.class);
Consumer<FileObject> next;
FileObject root;
FileSelector selector;

public FileCloneIngestor(FileObject root, Consumer<FileObject> next) {
this(root, new FileFilterSelector(), next);
}

public FileCloneIngestor(FileObject root, FileSelector selector, Consumer<FileObject> next) {
this.root = root;
this.selector = selector;
this.next = next;
}

@Override
public void accept(FileObject fileObject) {
try {
String filename = PrettyString.sanitize(fileObject.getPublicURIString());
FileObject newFile = root.resolveFile(filename);
log.info("file.clone: {} -> {}", fileObject.getPublicURIString(), newFile.getPublicURIString());

newFile.copyFrom(fileObject, selector);
if (next != null)
next.accept(newFile);
} catch (IOException e) {
log.error("file.clone.failed: {}", fileObject.getURI(), e);
throw new RuntimeException(e);
}
}
}
