package systems.symbol.platform;

import org.eclipse.rdf4j.repository.Repository;

public class Platform {

public void start() {
// default no-op
}

public void stop() {
// default no-op
}

public void shutdown() {
stop();
}

public void boot() {
// default no-op
}

public void X() {
// default no-op
}

public void XX() {
// default no-op
}

public void XXX() {
// default no-op
}

public void XXXX() {
// default no-op
}

public Repository getRepository(String name) {
throw new UnsupportedOperationException("Repository lookup not implemented");
}
}
