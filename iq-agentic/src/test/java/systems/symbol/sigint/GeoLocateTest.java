package systems.symbol.sigint;

import org.junit.jupiter.api.Test;
import systems.symbol.rdf4j.io.RDFDump;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeoLocateTest {

@Test
void locate() throws Exception {
GeoLocate geoLocate = new GeoLocate();

// Use a stable public IP value to avoid dependence on external call reliability in CI.
String result = geoLocate.location("8.8.8.8");

assertNotNull(result);
assertFalse(result.isBlank());
}
}