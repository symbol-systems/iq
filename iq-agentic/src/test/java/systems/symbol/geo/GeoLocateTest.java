package systems.symbol.geo;

import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import systems.symbol.rdf4j.io.RDFDump;

class GeoLocateTest {

    @Test
    void locate() throws Exception {
        GeoLocate geoLocate = new GeoLocate();

        Model located = geoLocate.locate();

        RDFDump.dump(located);

    }
}