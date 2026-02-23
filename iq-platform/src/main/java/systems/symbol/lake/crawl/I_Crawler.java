package systems.symbol.lake.crawl;

import org.eclipse.rdf4j.model.IRI;

import java.net.URI;
import java.util.function.Consumer;

public interface I_Crawler<T> {

public IRI crawl(URI from, Consumer<T> next);
}
