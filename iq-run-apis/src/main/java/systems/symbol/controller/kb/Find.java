package systems.symbol.controller.kb;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.RDFResponse;
import systems.symbol.finder.FactFinder;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.IOException;

@Path("find")
public class Find extends GuardedAPI {
    DynamicModelFactory dmf = new DynamicModelFactory();

    private final double DEFAULT_SCORE = 0.65;

    @GET
    @Path("{repo}/{finder}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response find(@PathParam("finder") String finder, @PathParam("repo") String repo,
            @QueryParam("query") String query,
            @QueryParam("score") double score, @QueryParam("max") int max,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        if (!Validate.isBearer(auth)) {
            log.info("kb.find#protected");
            return new OopsResponse("ux.iq.find#unauthorized", Response.Status.UNAUTHORIZED).build();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("ux.iq.find.indexer#repository", Response.Status.BAD_REQUEST).build();
        }
        if (Validate.isNonAlphanumeric(finder)) {
            return new OopsResponse("ux.iq.find.indexer#finder-invalid", Response.Status.BAD_REQUEST).build();
        }
        if (Validate.isMissing(query)) {
            return new OopsResponse("ux.iq.find.indexer#query-missing", Response.Status.BAD_REQUEST).build();
        }
        I_Realm realm = platform.getRealm(repo);
        if (realm == null)
            return new OopsResponse("ux.iq.find.realm", Response.Status.NOT_FOUND).build();
        Repository repository = realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.iq.find#repository", Response.Status.NOT_FOUND).build();
        FactFinder factFinder = realm.getFinder();
        if (factFinder == null) {
            return new OopsResponse("ux.iq.find.indexer#finder-missing", Response.Status.NOT_FOUND).build();
        }

        max = max == 0 ? 10 : Math.min(Math.max(max, 1), 10); // clamp 1-10
        score = score == 0 ? DEFAULT_SCORE : Math.min(Math.max(score, 0.1), 1.0); // clamp 0.1-1.0

        // retrieve results from SPARQL query to populate index
        try (RepositoryConnection connection = repository.getConnection()) {
            Model model = factFinder.search(dmf.createEmptyModel(), query, connection, max, score);
            if (model.isEmpty() && score == DEFAULT_SCORE) {
                log.info("kb.find#fallback");
                model = factFinder.search(dmf.createEmptyModel(), query, connection, max, DEFAULT_SCORE / 2);
            }
            return new RDFResponse(null, model, RDFFormat.JSONLD).build();

        } catch (Exception e) {
            return new OopsResponse("ux.iq.find.indexer#query-failed", e).build();
        }
    }

}
