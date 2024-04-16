package systems.symbol.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.*;
import org.eclipse.rdf4j.model.Model;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class GeoLocate {
    DatabaseReader reader;

    public GeoLocate(File db) throws IOException {
        init(db);
    }

    void init(File database) throws IOException {
        reader = new DatabaseReader.Builder(database).build();
    }

    public void lookup(String ip) throws IOException, GeoIp2Exception {
        InetAddress ipAddress = InetAddress.getByName(ip);
        CityResponse response = reader.city(ipAddress);
        Country country = response.getCountry();

        System.out.println(country.getIsoCode());
        System.out.println(country.getName());

        Subdivision subdivision = response.getMostSpecificSubdivision();
        System.out.println(subdivision.getName());
        System.out.println(subdivision.getIsoCode());

        City city = response.getCity();
        System.out.println(city.getName()); // 'Minneapolis'

        Postal postal = response.getPostal();
        System.out.println(postal.getCode()); // '55455'

        Location location = response.getLocation();
        System.out.println(location.getLatitude());  // 44.9733
        System.out.println(location.getLongitude()); // -93.2323
    }
}
