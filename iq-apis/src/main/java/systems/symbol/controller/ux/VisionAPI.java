package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

@Path("/ux/vision")
@Tag(name = "api.ux.vision.name", description = "api.ux.vision.description")
public class VisionAPI extends GuardedAPI {

@POST
@Operation(summary = "api.ux.vision.post.summary", description = "api.ux.vision.post.description")
@Path("{realm}/{path:.*}")
@Consumes(MediaType.APPLICATION_OCTET_STREAM)
public Response upload(@PathParam("realm") String _realm, @PathParam("path") String path,
@HeaderParam("Authorization") String auth) throws SecretsException {
if (Validate.isMissing(path)) {
return new OopsResponse("ux.vision#missing", Response.Status.BAD_REQUEST).build();
}
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.vision.realm", Response.Status.NOT_FOUND).build();
DecodedJWT jwt;
try {
jwt = authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}
log.info("ux.vision: {} -> {}", path, jwt.getSubject());

// File home = new File(platform.getInstance().getHome(), "vision");
// File file = new File(home, path);
// log.info("vision.upload: {} @ {} == {}", path, file, file.exists());
// if (!file.exists()) {
// return Response.status(Response.Status.NOT_FOUND)
// .entity(new SimpleResponse("error", "File not found").build())
// .build();
// }
return Response.ok().build();
}

}
