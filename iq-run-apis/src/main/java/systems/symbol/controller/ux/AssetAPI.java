package systems.symbol.controller.ux;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@Tag(name = "api.ux.assets.name", description = "api.ux.assets.description")
public class AssetAPI {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @GET
    @Operation(summary = "api.ux.assets.get.summary", description = "api.ux.assets.get.description")
    @Path("{path:.*}")
    public Response serveResource(@PathParam("path") String path) throws SecretsException {

        if (Validate.isMissing(path)) {
            return new OopsResponse("ux.path#missing", Response.Status.BAD_REQUEST).build();
        }

        String resourcePath = "public/" + path;
        log.info("Attempting to load resource: {}", resourcePath);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resourceURL = classLoader.getResource(resourcePath);

        if (resourceURL == null) {
            log.error("Resource not found: {}", resourcePath);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Determine MIME type
        String mimeType = URLConnection.guessContentTypeFromName(resourcePath);
        if (mimeType == null) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM; // Default fallback
        }
        log.info("Determined MIME type: {}", mimeType);

        try (InputStream resourceStream = resourceURL.openStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            // Read the resource into memory
            byte[] data = new byte[8192];
            int bytesRead;
            while ((bytesRead = resourceStream.read(data)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            byte[] content = buffer.toByteArray();

            return Response.ok(content)
                    .type(mimeType)
                    .header("Content-Length", content.length)
                    .build();

        } catch (IOException e) {
            log.error("Error reading resource: {}", resourcePath, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
