package systems.symbol.lake.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public abstract class AbstractConverter<T, Y> implements Consumer<T> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    Consumer<Y> next;

    public AbstractConverter() {
    }

    public AbstractConverter(Consumer<Y> next) {
        this.next = next;
    }

    abstract Y convert(T o) throws Exception;

    public void next(Y y) {
        // log.debug("next: {} -> {}", next!=null, y==null?false:y.toString());
        if (next != null && y != null)
            next.accept(y);
    }

    @Override
    public void accept(T t) {
        try {
            next(convert(t));
        } catch (Exception e) {
            log.error("convert.error: {}", t, e);
            throw new RuntimeException(e);
        }
    }
}
