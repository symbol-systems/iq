package systems.symbol.sigint;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;
import com.maxmind.geoip2.record.Location;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.WGS84;
import systems.symbol.string.PrettyString;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

public class GeoLocate {
    private static final String GEO_PREFIX = "geo:";
    private static final String SCHEMA_PREFIX = "http://schema.org/";
    private static final String RDF_SCHEMA_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    private static final String IPIFY_URL = "https://api.ipify.org";

    private DatabaseReader reader;

    public GeoLocate() throws IOException {
        load();
    }

    public GeoLocate(File db) throws IOException {
        load(db);
    }

    protected void load(File database) throws IOException {
        reader = new DatabaseReader.Builder(database).build();
    }

    protected void load() throws IOException {
        InputStream db = getClass().getClassLoader().getResourceAsStream("GeoLite2-City.mmdb");
        if (db == null) {
            throw new IOException("Database file not found");
        }
        reader = new DatabaseReader.Builder(db).build();
    }

    public static String getPublicIP() throws IOException {
        URL url = new URL(IPIFY_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    public String location() throws IOException, GeoIp2Exception {
        return location(getPublicIP());
    }

    public String location(String ip) throws IOException, GeoIp2Exception {
        InetAddress ipAddress = InetAddress.getByName(ip);
        CityResponse response = reader.city(ipAddress);
        return String.format("%s, %s, %s",
                response.getCity().getName(),
                response.getMostSpecificSubdivision().getName(),
                response.getCountry().getName());
    }

    public Model locate() throws IOException, GeoIp2Exception {
        String ip = getPublicIP();
        DynamicModelFactory dmf = new DynamicModelFactory();
        return locate(Values.iri("ip:v4:" + ip), ip, dmf.createEmptyModel());
    }

    public Model locate(IRI self, String ip, Model model) throws IOException, GeoIp2Exception {
        InetAddress ipAddress = InetAddress.getByName(ip);
        if (ipAddress.isAnyLocalAddress() || ipAddress.isLoopbackAddress()) {
            ipAddress = InetAddress.getByName(getPublicIP());
        }
        CityResponse response = reader.city(ipAddress);
        Location location = response.getLocation();

        IRI place = Values.iri(GEO_PREFIX + PrettyString.sanitize(response.getCountry().getName() + "_" +
                response.getLeastSpecificSubdivision().getName() + "_" +
                response.getMostSpecificSubdivision().getName() + "_" +
                response.getCity().getName()));
        Long geoNameId = response.getCity().getGeoNameId();
        String postalCode = response.getPostal() != null ? response.getPostal().getCode() : null;
        Integer metroCode = response.getLocation().getMetroCode();
        Integer populationDensity = response.getLocation().getPopulationDensity();
        String timeZone = response.getLocation().getTimeZone();
        Integer averageIncome = response.getLocation().getAverageIncome();
        Integer accuracyRadius = response.getLocation().getAccuracyRadius();

        ModelBuilder modelBuilder = new ModelBuilder(model);
        modelBuilder.subject(self)
                .add(RDF.TYPE, WGS84.SPATIAL_THING)
                .add(WGS84.LAT, location.getLatitude().toString())
                .add(WGS84.LONG, location.getLongitude().toString())
                .add(Values.iri(SCHEMA_PREFIX + "address"), place)
                .add(Values.iri(SCHEMA_PREFIX + "geoNameId"), geoNameId.toString())
                .add(Values.iri(SCHEMA_PREFIX + "postalCode"), postalCode !=null?postalCode:"")
                .add(Values.iri(SCHEMA_PREFIX + "metroCode"), metroCode != null ? metroCode.toString() : "")
                .add(Values.iri(SCHEMA_PREFIX + "populationDensity"), populationDensity != null ? populationDensity.toString() : "0")
                .add(Values.iri(SCHEMA_PREFIX + "timeZone"), timeZone)
                .add(Values.iri(SCHEMA_PREFIX + "averageIncome"), averageIncome != null ? averageIncome.toString() : "0")
                .add(Values.iri(SCHEMA_PREFIX + "accuracyRadius"), accuracyRadius != null ? accuracyRadius.toString() : "0")
                .build();

        if (response.getCountry() != null) {
            Country country = response.getCountry();
            modelBuilder.subject(self)
                    .add(schemaOrg("addressCountry"), Values.iri(RDF_SCHEMA_LABEL, country.getName()))
                    .build();
        }

        if (response.getMostSpecificSubdivision() != null) {
            Subdivision subdivision = response.getMostSpecificSubdivision();
            modelBuilder.subject(self)
                    .add(schemaOrg("addressRegion"), subdivision.getName())
                    .build();
        }

        if (response.getCity() != null) {
            City city = response.getCity();
            modelBuilder.subject(self)
                    .add(schemaOrg("addressLocality"), Values.iri(RDF_SCHEMA_LABEL, city.getName()))
                    .build();
        }

        if (response.getPostal() != null) {
            Postal postal = response.getPostal();
            modelBuilder.subject(self)
                    .add(schemaOrg("postalCode"), postal.getCode())
                    .build();
        }

        return modelBuilder.build();
    }

    private IRI schemaOrg(String name) {
        return Values.iri(SCHEMA_PREFIX + name);
    }
}
