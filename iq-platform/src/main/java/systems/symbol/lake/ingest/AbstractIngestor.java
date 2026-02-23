package systems.symbol.lake.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public abstract class AbstractIngestor<T> implements Consumer<T> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    Consumer<T> next;
    public AbstractIngestor() {}

    public AbstractIngestor(Consumer<T> next) {
        this.next = next;
    }
    public void next(T t) {
        if (next!=null) next.accept(t);
    }

    @Override
    abstract public void accept(T t);
}
