package systems.symbol.lake.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockIngestor<T> extends AbstractIngestor<T> {
private static final Logger log = LoggerFactory.getLogger(MockIngestor.class);
int seen = 0;

@Override

public void accept(T fileObject) {
seen++;
log.info("accept: #{}: {}", seen, fileObject);
}
}
