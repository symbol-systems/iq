package systems.symbol.service;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


class GeoLocateTest {

@Test
void lookup() throws IOException, GeoIp2Exception {
GeoLocate geoLocate = new GeoLocate(new File("db/GeoLite2-City.mmdb"));
geoLocate.lookup("101.113.207.95");
}
}