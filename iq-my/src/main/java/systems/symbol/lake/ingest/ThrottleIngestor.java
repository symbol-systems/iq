package systems.symbol.lake.ingest;

import java.util.function.Consumer;

public class ThrottleIngestor<T> extends AbstractIngestor<T> {
    private final int max;
    private int count = 0;
    private Consumer<T> next;

    public ThrottleIngestor(int max, Consumer<T> next) {
        this.max = max;
        this.next = next;
    }
    @Override
    public void accept(T o) {
        if (count++<max) {
            next.accept(o);
        }
    }
}
