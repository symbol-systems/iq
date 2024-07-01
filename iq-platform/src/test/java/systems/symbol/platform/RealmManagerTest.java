package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.Realms;
import systems.symbol.realm.RealmManager;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class RealmManagerTest {
IRI self = Values.iri("test:");
File home = new File("my.test");

@Test
void bootstrap() throws Exception {
RealmManager realms = new RealmManager();
Realms.bootstrap(realms, new File("src/test/resources/realms/").listFiles());
System.out.println("realm.bootstrap: "+realms.getRealms());
assert !realms.getRealms().isEmpty();
realms.stop();
}

@Test
void testRealm() throws Exception {
RealmManager realms = new RealmManager(home);
I_Realm realm = realms.getRealm(self);
System.out.println("realm.factory: "+realm.getSelf());
assertNotNull(realm);
assert realm.getSelf().equals(self);
realms.stop();
}

@Test
void index() throws Exception {
RealmManager realms = new RealmManager(home);
I_Realm realm = realms.getRealm(self);
System.out.println("realm.dump: "+realm.getSelf());
Model model = realm.getModel();
assert model!=null;
RDFDump.dump(realm.getModel());
assert model.size()==4;

Realms.index(realm, Values.iri("test:index"));

Collection<IRI> found = realm.getFinder().search("test", 10, 0.8);
assert found != null;
assert found.size() == 1;
assert found.contains(self);
realms.stop();
}
}