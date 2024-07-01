package systems.symbol.finder;

import java.util.Collection;

public interface I_Search<T> {
Collection<T> search(String text, int maxResults, double minScore);
}
