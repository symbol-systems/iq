package systems.symbol.controllers.search;

import systems.symbol.platform.APIPlatform;
import systems.symbol.responses.OopsResponse;
import systems.symbol.responses.SimpleResponse;
import systems.symbol.string.Validate;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("search/web")
public class SearchWeb {
@Inject
APIPlatform platform;

@GET
@Path("{provider}")
@Produces(MediaType.APPLICATION_JSON)
public Response search(@PathParam("provider") String provider, @QueryParam("query") String query,
@QueryParam("maxResults") int maxResults , @QueryParam("relevancy") double relevancy) {
if (Validate.isNonAlphanumeric(provider)) {
return new OopsResponse("api.search.web#provider-invalid", Response.Status.BAD_REQUEST).asJSON();
}
return new SimpleResponse("api.search.web:"+provider+"?query="+query).asJSON();
}
}

