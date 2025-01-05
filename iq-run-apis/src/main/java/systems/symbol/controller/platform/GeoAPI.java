package systems.symbol.controller.platform;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.eclipse.rdf4j.model.IRI;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.vertx.ext.web.RoutingContext;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.sigint.GeoLocate;

/**
 * RESTful endpoint for checking the geo-location of the request.
 */
@Path("geo")
public class GeoAPI extends RealmAPI {
@Context
RoutingContext routing;

/**
 * Identifies the IPv4 of request and looks up geo-location.
 *
 * @return GeoLocation indicating the requests approx location.
 */
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response locate(@Context UriInfo info, @Context HttpHeaders headers) {
Bindings reply = new SimpleBindings();
try {
GeoLocate geo = new GeoLocate();
String ipv4 = routing.request().remoteAddress().host();
reply.putIfAbsent("ipv4", ipv4);
if (!ipv4.equals("127.0.0.1")) {
reply.putIfAbsent("client", geo.location(ipv4));
} else {
reply.putIfAbsent("client", geo.location());
}
reply.putIfAbsent("server", geo.location());
log.info("api.geo.ipv4: {}", reply);
return new SimpleResponse(reply).build();
} catch (Exception e) {
log.warn("api.geo.oops.geo: {} -> {}", e.getMessage(), e);
return new OopsResponse("api.geo.locate", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

@Override
public boolean entitled(DecodedJWT jwt, IRI agent) {
return true;
}

}
