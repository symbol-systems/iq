package systems.symbol.lake.crawl;

import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.function.Consumer;

public class Walkers {
    private static final Logger log = LoggerFactory.getLogger(Walkers.class);

    public static FileObject relocate(FileObject from_root, FileObject file_from, FileObject to_root) {
        if (!file_from.getName().getURI().startsWith(from_root.getName().getURI())) return null;
        String path = file_from.getName().getURI().substring(from_root.getName().getURI().length()+1);
        try {
            return to_root.resolveFile(path);
        } catch (FileSystemException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static void recurse(FileSystemManager vfs, FileSystemOptions opts, String rootFolder, Consumer<FileObject> consumer) throws FileSystemException {
        FileObject root = vfs.resolveFile(rootFolder, opts);
        log.info("recurse.start: {} -> {}", rootFolder, root.getURI());
        recurse(root, consumer);
        log.info("recurse.end: "+root.getURI());
    }

    private static void recurse(FileObject file, Consumer<FileObject> consumer) throws FileSystemException {
        if (file.isFile()) {
            consumer.accept(file);
        } else if (file.isFolder()) {
            FileObject[] children = file.getChildren();
            for (FileObject child : children) {
                recurse(child, consumer);
            }
        }
    }
}
