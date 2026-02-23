package systems.symbol.lake.ingest;

import dev.langchain4j.data.embedding.Embedding;
import systems.symbol.finder.I_Finder;
import systems.symbol.lake.ContentEntity;

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
Embedding stored = finder.store(ce.getSelf().stringValue(), ce.getContent().toString());
if (stored == null)
return;
next(ce);
}

}
