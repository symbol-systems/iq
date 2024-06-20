package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.string.Validate;

import java.io.InputStream;

@Path("/ux/generate")
@Tag(name = "api.ux.generate.name", description = "api.ux.generate.description")
public class GenerateAPI extends GuardedAPI {

@POST
@Operation(
summary = "api.ux.generate.post.summary",
description = "api.ux.generate.post.description"
)
@Path("{actor:.*}")
@Consumes(MediaType.APPLICATION_OCTET_STREAM)
public Response generate(@PathParam("actor") String actor, @HeaderParam("Authorization") String auth, @Context UriInfo uriInfo) {
DecodedJWT jwt;
try {
jwt = authenticate(auth);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}

return new DataResponse("ok").asJSON();
}
}
