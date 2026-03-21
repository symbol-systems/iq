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
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.File;

@Path("/ux/ipfs")
@Tag(name = "api.ux.files.name", description = "api.ux.files.description")
public class FilesAPI extends GuardedAPI {

@GET
@Operation(summary = "api.ux.files.get.summary", description = "api.ux.files.get.description")
@Path("{realm}/{ipfs:.*}")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public Response download(@PathParam("realm") String _realm, @PathParam("ipfs") String ipfs,
@HeaderParam("Authorization") String auth) throws SecretsException {
if (Validate.isMissing(ipfs)) {
return new OopsResponse("ux.ipfs#missing", Response.Status.BAD_REQUEST).build();
}
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.ipfs.realm", Response.Status.NOT_FOUND).build();
DecodedJWT jwt;
try {
jwt = authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}
log.info("ux.ipfs: {} -> {}", ipfs, jwt.getSubject());

File home = new File(platform.getInstance().getHome().getPath().toString(), "vfs");
File file = new File(home, ipfs);
log.info("ipfs.download: {} @ {} == {}", ipfs, file, file.exists());
if (!file.exists()) {
return Response.status(Response.Status.NOT_FOUND)
.entity(new SimpleResponse("error", "File not found").build())
.build();
}
return Response.ok(file)
.header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"")
.build();
}
}
