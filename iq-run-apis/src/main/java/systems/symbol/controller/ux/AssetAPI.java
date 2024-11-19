package systems.symbol.controller.ux;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@Tag(name = "api.ux.assets.name", description = "api.ux.assets.description")
public class AssetAPI {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @GET
    @Operation(summary = "api.ux.assets.get.summary", description = "api.ux.assets.get.description")
    @Path("{path:.*}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response serve(@PathParam("path") String path) throws SecretsException {

        if (Validate.isMissing(path)) {
            return new OopsResponse("ux.path#missing", Response.Status.BAD_REQUEST).build();
        }

        // Resolve file path from MY_ASSET_PATH environment variable
        String assetPath = System.getenv("MY_ASSET_PATH");
        if (assetPath == null || assetPath.isEmpty()) {
            assetPath = "public";
        }

        File file = new File(new File(assetPath), path);
        log.info("ipfs.download: {} @ {} == {}", path, file.getAbsolutePath(), file.exists());

        // Check if file exists
        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Return the file as a content response
        return Response.ok(file).build();
    }
}
