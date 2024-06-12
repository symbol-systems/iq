package systems.symbol.controller.ux;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;

import java.io.InputStream;

@Path("/moat")
public class GenerateAPI extends GuardedAPI {

    @POST
    @Path("/upload/{repo}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@PathParam("repo") String repo, @FormParam("file") InputStream in) {


        return new DataResponse("ok").asJSON();
    }
}
