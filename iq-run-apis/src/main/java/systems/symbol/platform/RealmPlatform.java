package systems.symbol.platform;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.I_Realms;
import systems.symbol.realm.Realms;
import systems.symbol.realm.RealmManager;
import systems.symbol.secrets.SecretsException;

import java.util.Set;

//@ApplicationScoped
@Singleton
public class RealmPlatform  implements I_Realms {
protected static final Logger log = LoggerFactory.getLogger(RealmPlatform.class);

static RealmManager realms;
static int count = 0;

static {
try {
realms = new RealmManager();
log.info("realms.static: {}", ++count);
} catch (Exception e) {
throw new RuntimeException(e);
}
}

public RealmPlatform() throws Exception {
}

public RealmManager getInstance() {
return realms;
}


void onStart(@Observes StartupEvent ev) {
try {
log.info("realms.onStart: {}", ev);
realms.start();
log.info("realms.bootstrap: {} -> {}", realms.getHome().getAbsolutePath() , realms.getRealms());
Realms.bootstrap(realms);
log.info("realm.booted: {}", realms.getRealms());
Realms.index(realms);
log.info("realms.indexed: {}", realms.getRealms());
} catch (Exception e) {
throw new RuntimeException(e);
}
}

void onStop(@Observes ShutdownEvent ev) {
log.info("realms.onStop: {} -> {}", ev.isStandardShutdown(), ev);
realms.stop();
}

public I_Realm getRealm(String self) throws SecretsException {
log.debug("realm.getRealm: {} -> {}", self, realms.getRealms());
return realms.getRealm(self);
}

@Override
public I_Realm getRealm(IRI self) throws SecretsException {
return realms.getRealm(self);
}

@Override
public I_Realm getRealm(IRI self, Model model) throws SecretsException {
return realms.getRealm(self, model);
}

@Override
public Set<IRI> getRealms() {
return realms.getRealms();
}
}
