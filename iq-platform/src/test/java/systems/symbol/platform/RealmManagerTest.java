package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.RealmManager;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class RealmManagerTest {

@Test
void getSpace() throws Exception {
IRI self = Values.iri(IQ_NS.TEST);
RealmManager realmManager = new RealmManager(self, new File("my.test"));
I_Realm realm = realmManager.getRealm(self);
System.out.println("realm.factory: "+realm);
assertNotNull(realm);
}

@Test
void boot() {
}

@Test
void testGetSpace() {
}

@Test
void testGetSpace1() {
}

@Test
void getRepository() {
}
}