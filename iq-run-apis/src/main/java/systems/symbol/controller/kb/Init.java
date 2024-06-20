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
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.string.Validate;

import java.io.IOException;

@Path("init")
public class Init extends GuardedAPI {

/**
 * Initializes a new repository of the specified type.
 * The repository type and name are provided as path parameters.
 *
 * @param type The type of the new repository.
 * @param repo The name of the new repository.
 * @return A JSON response indicating the result of the initialization operation.
 */
@Path("{repo}/{type}")
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response initRepository(@PathParam("type") String type,
   @PathParam("repo") String repo,
   @HeaderParam("Authorization") String auth) {
if (!Validate.isBearer(auth)) {
log.info("kb.find#protected");
return new OopsResponse("api.init#unauthorized", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.init#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isNonAlphanumeric(type)) {
return new OopsResponse("api.init#type-invalid", Response.Status.BAD_REQUEST).asJSON();
}
Repository repository = null;
try {
repository = this.platform.getWorkspace().create(repo, type);
} catch (IOException e) {
log.error("api.init.broken: {}", e.getMessage(), e);
}

if (repository == null || !repository.isInitialized()) {
return new OopsResponse("api.init.broken", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}

return new DataResponse("api.init.ok").asJSON();
}
}
