package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.eclipse.rdf4j.model.IRI;
import com.auth0.jwt.interfaces.DecodedJWT;

import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.platform.WebURLs;
import systems.symbol.sigint.GeoLocate;

/**
 * RESTful endpoint for checking the geo-location of the request.
 */
@Path("geo")
public class GeoAPI extends RealmAPI {
private static final String LOCALHOST_IPV4 = "127.0.0.1";

/**
 * Check if the given IP address is a loopback/localhost address.
 * @param ipv4 The IP address to check
 * @return true if the IP is localhost or loopback
 */
private boolean isLocalhost(String ipv4) {
if (ipv4 == null) return false;
return LOCALHOST_IPV4.equals(ipv4) || ipv4.startsWith("127.") || "::1".equals(ipv4);
}

// @Context
// RoutingContext routing;

/**
 * Identifies the IPv4 of request and looks up geo-location.
 *
 * @return GeoLocation indicating the requests approx location.
 */
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response locate(@Context UriInfo info, @Context HttpHeaders headers) {
Bindings reply = new SimpleBindings();
ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("GMT"));
reply.putIfAbsent("now", System.currentTimeMillis());
reply.putIfAbsent("gmt", now.format(DateTimeFormatter.RFC_1123_DATE_TIME));
String ipv4 = WebURLs.getClientIP(null, headers);
if (ipv4 != null) {
reply.putIfAbsent("ipv4", ipv4);
try {
GeoLocate geo = new GeoLocate();
if (!isLocalhost(ipv4)) {
// Only geolocate non-localhost addresses (avoids unnecessary queries for local development)
reply.putIfAbsent("client", geo.location(ipv4));
} else {
reply.putIfAbsent("client", geo.location());
}
reply.putIfAbsent("server", geo.location());
// log.info("api.geo.ipv4: {}", reply);
} catch (Exception e) {
log.warn("api.geo.oops.geo: {} -> {}", e.getMessage(), ipv4);
reply.putIfAbsent("error", e.getMessage());
}
}
return new SimpleResponse(reply).build();
}

@Override
public boolean entitled(DecodedJWT jwt, IRI agent) {
return true;
}

}
