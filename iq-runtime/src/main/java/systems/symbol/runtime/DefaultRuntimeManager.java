package systems.symbol.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.realm.Realm;

/**
 * Default implementation of RuntimeManager for Quarkus-based runtime.
 */
public class DefaultRuntimeManager implements RuntimeManager {
private static final Logger log = LoggerFactory.getLogger(DefaultRuntimeManager.class);

private Realm realm;
private volatile boolean active = false;

@Override
public void initialize(Realm realm) throws Exception {
log.info("runtime.init: {}", realm.getSelf());
this.realm = realm;
}

@Override
public void start() throws Exception {
log.info("runtime.start");
active = true;
}

@Override
public void stop() throws Exception {
log.info("runtime.stop");
active = false;
}

@Override
public boolean isActive() {
return active;
}

@Override
public String getStatus() {
return active ? "ACTIVE" : "STOPPED";
}
}
