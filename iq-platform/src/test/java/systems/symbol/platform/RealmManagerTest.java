package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import systems.symbol.finder.FactFinder;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.realm.*;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class RealmManagerTest {
IRI self = Values.iri("test:");
File home = new File("my.test");

@Test
void bootstrap() throws Exception, PlatformException {
RealmManager realms = new RealmManager();
Realms.bootstrap(realms, new File("src/test/resources/realms/").listFiles());
System.out.println("realm.bootstrap: "+realms.getRealms());
assert !realms.getRealms().isEmpty();
realms.stop();
}

@Test
void testRealmFactory() throws Exception, PlatformException {
RealmManager realms = new RealmManager(home);
I_Realm realm = realms.getRealm(self);
assert realm == null;
assert realms.newRealm(self).getSelf().equals(self);
realm = realms.getRealm(self);
assertNotNull(realm);
assert realm.getSelf().equals(self);
realms.stop();
}

@Test
void index() throws Exception {
RealmManager realms = new RealmManager(home);
TreeModel model = new TreeModel();
ValueFactory vf = SimpleValueFactory.getInstance();
model.add( vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.***REMOVED***("test case")));
model.add( vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.***REMOVED***("some case")));
model.add( vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.***REMOVED***("hello world")));
model.add( vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.***REMOVED***("another world")));
assert model.size()==4;

//FactFinder finder = new
//Facts.index(model, Values.iri("test:index"));

//Collection<IRI> found = realm.getFinder().search("test", 10, 0.8);
//assert found != null;
//assert found.size() == 1;
//assert found.contains(self);
realms.stop();
}
}