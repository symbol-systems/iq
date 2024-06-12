package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.SpaceManager;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SpaceManagerTest {

    @Test
    void getSpace() throws Exception {
        IRI self = Values.iri(IQ_NS.TEST);
        SpaceManager spaceManager = new SpaceManager(self, new File("my.test"));
        I_Realm space = spaceManager.getSpace(self);
        System.out.println("space.factory: "+space);
        assertNotNull(space);
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