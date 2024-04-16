package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.rdf4j.io.IOCopier;
import org.apache.commons.vfs2.FileObject;

import java.io.IOException;
import java.util.function.Consumer;

public class FileContentConverter extends AbstractConverter<FileObject, ContentEntity<String>> {
    public FileContentConverter(Consumer<ContentEntity<String>> next) {
        super(next);
    }

    public ContentEntity<String> convert(FileObject fileObject) throws IOException {
        String content = IOCopier.toString(fileObject.getContent().getInputStream());
        ContentEntity<String> entity = new ContentEntity<>(fileObject.getPublicURIString(), content);
        log.debug("converted: {} x {}", entity.getSelf(), entity.getContent().length());
        return entity;
    }

}
