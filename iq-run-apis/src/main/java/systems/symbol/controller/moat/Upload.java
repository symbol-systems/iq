package systems.symbol.controller.moat;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;

import java.io.InputStream;

@Path("/moat/upload")
public class Upload extends GuardedAPI {

@POST
@Path("{repo}")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadFile(@PathParam("repo") String repo, @FormParam("file") InputStream input) {

return new DataResponse("ok").build();
}
}
