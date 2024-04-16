package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.eclipse.rdf4j.model.IRI;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class DeDupeIngestor<T> extends AbstractIngestor<ContentEntity<T>> {
    Set<IRI> seen = new HashSet<>();
    int dupes = 0;
    public DeDupeIngestor(Consumer<ContentEntity<T>> consumer) {
        super(consumer);
    }

    @Override
    public void accept(ContentEntity<T> file) {
        IRI uri = file.getSelf();
        if (!seen.contains(uri)) {
            next(file);
            seen.add(uri);
        } else dupes++;
    }

    public int getDuplicateCount() {
        return this.dupes;
    }

    public Set<IRI> getSeen() {
        return this.seen;
    }
}
