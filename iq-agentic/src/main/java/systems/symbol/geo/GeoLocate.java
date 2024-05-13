package systems.symbol.geo;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.WGS84;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

public class GeoLocate {
private DatabaseReader reader;

public GeoLocate() throws IOException {
load(new File("db/GeoLite2-City.mmdb"));
}

public GeoLocate(File db) throws IOException {
load(db);
}

protected void load(File database) throws IOException {
reader = new DatabaseReader.Builder(database).build();
}

public static String getPublicIP() throws IOException {
URL url = new URL("https://api.ipify.org");

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

public Model locate() throws IOException, GeoIp2Exception {
String ip = getPublicIP();
DynamicModelFactory dmf = new DynamicModelFactory();
return locate(Values.iri("urn:ip:"+ip), ip, dmf.createEmptyModel());
}

public Model locate(IRI self, String ip, Model model) throws IOException, GeoIp2Exception {
InetAddress ipAddress = InetAddress.getByName(ip);
CityResponse response = reader.city(ipAddress);

ModelBuilder modelBuilder = new ModelBuilder(model);
modelBuilder.subject(self)
.add(RDF.TYPE, WGS84.SPATIAL_THING)
.add(WGS84.LAT, response.getLocation().getLatitude().toString())
.add(WGS84.LONG, response.getLocation().getLongitude().toString())
.build();

if (response.getCountry() != null) {
Country country = response.getCountry();
modelBuilder.subject(self)
.add(schemaOrg("addressCountry"), country.getName())
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
.add(schemaOrg("addressLocality"), city.getName())
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
return Values.iri("http://schema.org/"+name);
}
}
