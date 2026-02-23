package systems.symbol.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class Consumers<T> implements Consumer<T> {
    private static final Logger log = LoggerFactory.getLogger(Consumers.class);
    List<Consumer<T>> chain;

    public Consumers() {
        chain = new ArrayList<Consumer<T>>();
    }

    public Consumers(Consumer<T> processor) {
        this();
        chain.add(processor);
    }

    public Consumers<T> add(Consumer<T> processor) {
        chain.add(processor);
        return this;
    }

    public void accept(T file) {
        for (Consumer<T> consumer : chain) {
            log.debug("ingest: " + file + " with " + consumer.getClass().getSimpleName());
            consumer.accept(file);
        }
    }
}
