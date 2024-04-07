package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.finder.I_Finder;
import dev.langchain4j.data.embedding.Embedding;

import java.util.function.Consumer;

public class FinderIngestor<T> extends AbstractIngestor<ContentEntity<T>> {
    I_Finder finder;

    public FinderIngestor(I_Finder finder) {
        this.finder = finder;
    }

    public FinderIngestor(I_Finder finder, Consumer<ContentEntity<T>> processor) {
        super(processor);
        this.finder = finder;
    }

    @Override
    public void accept(ContentEntity<T> ce) {
        Embedding stored = finder.store(ce.getIdentity().stringValue(), ce.getContent().toString());
        if (stored==null) return;
        next(ce);
    }

}
