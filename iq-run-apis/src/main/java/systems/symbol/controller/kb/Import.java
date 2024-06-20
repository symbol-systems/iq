/**
 * The BulkImport class provides RESTful endpoints for bulk importing assets and initializing new repositories.
 * It is part of the Knowledge Base API.
 */
package systems.symbol.controller.kb;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.io.BootstrapLoader;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.string.PrettyString;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.SimpleBindings;
import java.io.File;
import java.io.IOException;

@Path("import")
public class Import extends GuardedAPI {

/**
 * Imports assets from the local filesystem into the specified repository.
 * The assets are loaded from the provided path, and the operation is idempotent.
 *
 * @param repo The name of the repository to import into.
 * @return A JSON response indicating the result of the import operation.
 */
@POST
@Path("{repo}")
@Produces(MediaType.APPLICATION_JSON)
public Response importLocal(@PathParam("repo") String repo, @QueryParam("clean")  Boolean clean,
@HeaderParam("Authorization") String auth) throws IOException {
try {
return deploy(repo, auth, clean != null && clean);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
}

public Response deploy(String repo, String auth, boolean clean) throws IOException, OopsException {
//DecodedJWT jwt = authenticate(auth, "roles", new String[] { });
if (Validate.isNonAlphanumeric(repo)) {
throw new OopsException("api.import.invalid-repository", Response.Status.BAD_REQUEST);
}
Repository repository = platform.getRepository(repo);
if (repository == null) {
throw new OopsException("api.import.repository-missing", Response.Status.NOT_FOUND);
}
if (!repository.isInitialized()) {
throw new OopsException("api.import.offline", Response.Status.SERVICE_UNAVAILABLE);
}

File assetHome = new File(platform.getImportsHome(), PrettyString.sanitize(repo));
assetHome.mkdirs();

Stopwatch stopwatch = new Stopwatch();
try (RepositoryConnection connection = repository.getConnection()) {
if (clean) connection.clear();

BootstrapLoader loader = new BootstrapLoader(platform.getSelf().stringValue(), connection);
loader.deploy(assetHome);

SimpleBindings result = new SimpleBindings();
result.put("realm", repo);
DataResponse response = new DataResponse(result);
response.set("files", loader.total_files);
response.set("assets", loader.total_asset_files);
response.set("rdf", loader.total_rdf_files);
response.set("facts", connection.size());
response.set("elapsed", stopwatch.elapsed());
connection.commit();
return response.asJSON();
} catch (IOException e) {
return new OopsResponse("api.import.assets.faulty#" + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR).asJSON();
} catch (Exception e) {
throw new RuntimeException(e);
}
}

}
