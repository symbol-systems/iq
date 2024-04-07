/**
 * The BulkImport class provides RESTful endpoints for bulk importing assets and initializing new repositories.
 * It is part of the Knowledge Base API.
 */
package systems.symbol.controllers.kb;

import systems.symbol.platform.APIPlatform;
import systems.symbol.rdf4j.io.BulkAssetLoader;
import systems.symbol.responses.OopsResponse;
import systems.symbol.responses.SimpleResponse;
import systems.symbol.string.Validate;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.Repository;

import java.io.File;
import java.io.IOException;

@Path("import")
public class BulkImport {

    @Inject
    APIPlatform platform;

    /**
     * Imports assets from the local filesystem into the specified repository.
     * The assets are loaded from the provided path, and the operation is idempotent.
     *
     * @param repo The name of the repository to import into.
     * @param path The path to the directory containing the assets.
     * @return A JSON response indicating the result of the import operation.
     * @throws IOException If an error occurs during the import process.
     */
    @GET
    @Path("local/{repo}/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response importLocal(@PathParam("repo") String repo, @PathParam("path") String path) throws IOException {
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.import#invalid-repository", Response.Status.BAD_REQUEST).asJSON();
        }
        if (!Validate.isRelativePath(path)) {
            return new OopsResponse("api.import#invalid-path", Response.Status.BAD_REQUEST).asJSON();
        }

        Repository repository = platform.getWorkspace().alwaysGetRepository(repo);

        if (repository == null) {
            return new OopsResponse("api.import#repository-missing", Response.Status.NOT_FOUND).asJSON();
        }

        if (!repository.isInitialized()) {
            return new OopsResponse("api.import.offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
        }

        File assetHome = new File(path);

        if (!assetHome.exists()) {
            return new OopsResponse("api.import.assets.missing", Response.Status.NO_CONTENT).asJSON();
        }

        BulkAssetLoader loader = new BulkAssetLoader(platform.getWorkspace().getOwnerNamespace(), repository.getConnection());

        try {
            loader.deploy(assetHome);
        } catch (IOException e) {
            return new OopsResponse("api.import.assets.deploy#" + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR).asJSON();
        }

        String msg = "files=" + loader.total_files + ", asset_files" + loader.total_asset_files + ", rdf_files:" + loader.total_rdf_files;
        return new SimpleResponse(msg).asJSON();
    }

    /**
     * Initializes a new repository of the specified type.
     * The repository type and name are provided as path parameters.
     *
     * @param type The type of the new repository.
     * @param repo The name of the new repository.
     * @return A JSON response indicating the result of the initialization operation.
     * @throws IOException If an error occurs during the initialization process.
     */
    @Path("new/{type}/{repo}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response initRepository(@PathParam("type") String type, @PathParam("repo") String repo) throws IOException {
        Repository repository = this.platform.getWorkspace().create(repo, type);

        if (repository == null || !repository.isInitialized()) {
            return new SimpleResponse("api.import.new.broken").asJSON();
        }

        return new SimpleResponse("api.import.new.ok").asJSON();
    }
}
