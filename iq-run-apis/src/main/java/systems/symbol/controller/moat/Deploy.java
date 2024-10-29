/**
 * The BulkImport class provides RESTful endpoints for bulk importing assets and initializing new repositories.
 * It is part of the Knowledge Base API.
 */
package systems.symbol.controller.moat;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import com.auth0.jwt.interfaces.DecodedJWT;

import systems.symbol.controller.platform.RealmAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.RealmManager;
import systems.symbol.realm.Realms;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.SimpleBindings;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

@Path("/moat/deploy")
public class Deploy extends RealmAPI {

    /**
     * Imports assets from the local filesystem into the specified repository.
     * The assets are loaded from the provided path, and the operation is
     * idempotent.
     *
     * @param repo The name of the repository to import into.
     * @return A JSON response indicating the result of the import operation.
     */
    @POST
    @Path("{realm}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deploy(@PathParam("realm") String _realm, @QueryParam("clean") Boolean clean,
            @HeaderParam("Authorization") String auth) throws IOException {
        try {
            return redeploy(_realm, auth, clean != null && clean);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return new OopsResponse(e.getMessage(), Response.Status.BAD_REQUEST).build();
        } catch (SecretsException e) {
            e.printStackTrace();
            return new OopsResponse(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response redeploy(String _realm, String auth, boolean clean)
            throws IOException, OopsException, URISyntaxException, SecretsException {
        // DecodedJWT jwt = authenticate(auth, "roles", new String[] { });
        if (Validate.isNonAlphanumeric(_realm)) {
            throw new OopsException("ux.lake.deploy.invalid-repository", Response.Status.BAD_REQUEST);
        }
        I_Realm realm = platform.getRealm(_realm);
        if (realm == null) {
            throw new OopsException("ux.lake.deploy.realm", Response.Status.NOT_FOUND);
        }
        Repository repository = realm.getRepository();
        if (repository == null) {
            throw new OopsException("ux.lake.deploy.repository", Response.Status.NOT_FOUND);
        }
        if (!repository.isInitialized()) {
            throw new OopsException("ux.lake.deploy.offline", Response.Status.SERVICE_UNAVAILABLE);
        }

        RealmManager realms = platform.getInstance();
        File lake = new File(realms.getLakeHome(), _realm);
        log.info("ux.lake.deploy.lake: {} -> {}", realm.getSelf(), lake.getAbsolutePath());

        Stopwatch stopwatch = new Stopwatch();
        try (RepositoryConnection connection = repository.getConnection()) {
            if (clean)
                connection.clear();

            log.info("ux.lake.deploy.boot: {} -> {}", stopwatch, connection.size());
            Realms.boot(realm, lake);
            log.info("ux.lake.deploy.ok: {} -> {}", realm.getSelf().getLocalName(), lake);
            SimpleBindings result = new SimpleBindings();
            DataResponse response = new DataResponse(result);
            // response.set("files", loader.total_files);
            // response.set("assets", loader.total_asset_files);
            // response.set("rdf", loader.total_rdf_files);
            response.set("realm", realm.getSelf().stringValue());
            response.set("size", connection.size());
            response.set("elapsed", stopwatch.elapsed());
            connection.commit();
            return response.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        return true;
    }

}
