/**
 * The BulkImport class provides RESTful endpoints for bulk importing assets and initializing new repositories.
 * It is part of the Knowledge Base API.
 */
package systems.symbol.controller.kb;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.Repository;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.rdf4j.io.BootstrapLoader;
import systems.symbol.string.Validate;

import java.io.File;
import java.io.IOException;

@Path("import")
public class Import extends GuardedAPI {

/**
 * Imports assets from the local filesystem into the specified repository.
 * The assets are loaded from the provided path, and the operation is idempotent.
 *
 * @param repo The name of the repository to import into.
 * @param path The path to the directory containing the assets.
 * @return A JSON response indicating the result of the import operation.
 */
@GET
@Path("{repo}/{path: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response importLocal(@PathParam("repo") String repo,
@PathParam("path") String path,
@HeaderParam("Authorization") String auth) throws IOException {
if (!Validate.isBearer(auth)) {
log.info("api.import#protected");
if (!Validate.isUnGuarded())
return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
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

BootstrapLoader loader = new BootstrapLoader(platform.getWorkspace().getOwnerNamespace(), repository.getConnection());

try {
loader.deploy(assetHome);
} catch (IOException e) {
return new OopsResponse("api.import.assets.deploy#" + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}

String msg = "files=" + loader.total_files + ", asset_files" + loader.total_asset_files + ", rdf_files:" + loader.total_rdf_files;
return new SimpleResponse(msg).asJSON();
}

}
