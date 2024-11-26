package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import com.github.jsonldjava.shaded.com.google.common.io.Files;

import systems.symbol.lake.Lakes;
import systems.symbol.realm.*;

class RealmManagerTest {
    IRI self = Values.iri("test:");

    @Test
    void testBootstrapRealm() throws Exception, PlatformException {
        RealmManager realms = new RealmManager();
        Lakes.boot(realms);
        System.out.println("realm.bootstrap: " + realms.getRealms());
        assert !realms.getRealms().isEmpty();
        assert realms.newRealm(self).getSelf().equals(self);
        realms.stop();
    }

    @Test
    void testRealmindexer() throws Exception {
        RealmManager realms = new RealmManager(Files.createTempDir());
        TreeModel model = new TreeModel();
        ValueFactory vf = SimpleValueFactory.getInstance();
        model.add(vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.literal("test case")));
        model.add(vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.literal("some case")));
        model.add(vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.literal("hello world")));
        model.add(vf.createStatement(Values.iri(IQ_NS.TEST), RDF.VALUE, Values.literal("another world")));
        assert model.size() == 4;

        // FactFinder finder = new
        // Facts.index(model, Values.iri("test:index"));

        // Collection<IRI> found = realm.getFinder().search("test", 10, 0.8);
        // assert found != null;
        // assert found.size() == 1;
        // assert found.contains(self);
        realms.stop();
    }
}