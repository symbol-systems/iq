package systems.symbol.sigint;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Subdivision;

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
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeoLocate {
    private static final String GEO_PREFIX = "geo:";
    private static final String SCHEMA_PREFIX = "http://schema.org/";
    // private static final String RDF_SCHEMA_LABEL =
    // "http://www.w3.org/2000/01/rdf-schema#label";
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
        URI uri = URI.create(IPIFY_URL);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
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

        String city = response.getCity() != null ? response.getCity().getName() : null;
        String subdivision = response.getMostSpecificSubdivision() != null
                ? response.getMostSpecificSubdivision().getName()
                : null;
        String country = response.getCountry() != null ? response.getCountry().getName() : null;

        return Stream.of(city, subdivision, country)
                .filter(Objects::nonNull) // Remove null values
                .filter(s -> !s.isEmpty()) // Remove empty strings
                .collect(Collectors.joining(", "));
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
        Country country = response.getCountry();

        ModelBuilder modelBuilder = new ModelBuilder(model);
        modelBuilder.subject(self)
                .add(RDF.TYPE, WGS84.SPATIAL_THING)
                .add(WGS84.LAT, location.getLatitude().toString())
                .add(WGS84.LONG, location.getLongitude().toString())
                .add(Values.iri(SCHEMA_PREFIX + "address"), place)
                .add(Values.iri(SCHEMA_PREFIX + "postalCode"), postalCode != null ? postalCode : "")
                .add(Values.iri(SCHEMA_PREFIX + "addressCountry"), country != null ? country.getName() : "")
                .add(Values.iri(SCHEMA_PREFIX + "geoNameId"), geoNameId != null ? geoNameId.toString() : "")
                .add(Values.iri(SCHEMA_PREFIX + "metroCode"), metroCode != null ? metroCode.toString() : "")
                .add(Values.iri(SCHEMA_PREFIX + "populationDensity"),
                        populationDensity != null ? populationDensity.toString() : "0")
                .add(Values.iri(SCHEMA_PREFIX + "timeZone"), timeZone)
                .add(Values.iri(SCHEMA_PREFIX + "averageIncome"),
                        averageIncome != null ? averageIncome.toString() : "0")
                .add(Values.iri(SCHEMA_PREFIX + "accuracyRadius"),
                        accuracyRadius != null ? accuracyRadius.toString() : "0")
                .build();
        return modelBuilder.build();
    }

    // private IRI schemaOrg(String name) {
    // return Values.iri(SCHEMA_PREFIX + name);
    // }
}
