package systems.symbol.controller.search;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.*;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.finder.FactFinder;
import systems.symbol.platform.APIPlatform;
import systems.symbol.finder.IndexHelper;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.rdf4j.util.UsefulSPARQL;
import systems.symbol.string.Validate;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.util.Stopwatch;

import javax.script.SimpleBindings;
import java.io.IOException;

@Path("index")
public class TextIndexer extends GuardedAPI {
    @POST
    @Path("{repo}/{finder}/{query: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response importLocal(@PathParam("repo")String repo, @PathParam("finder")String finder,
                                @PathParam("query") String query,
                                @HeaderParam("Authorization") String auth) throws IOException {
        return doImport(repo, finder, query, auth);
    }

    @POST
    @Path("{repo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response importLocal(@PathParam("repo")String repo,
                                @HeaderParam("Authorization") String auth) throws IOException {
        return doImport(repo, repo, platform.getSelf()+"iq/indexer", auth);
    }

    public Response doImport(String repo,
                String finder, String query,
                String auth) throws IOException {
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
        }
        log.info("text.index: {} -> {} -> {}", repo, finder, query);

        if (Validate.isNonAlphanumeric(finder)) {
            return new OopsResponse("api.iq.text.indexer#finder-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.iq.text.indexer#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (!Validate.isURN(query)) {
            return new OopsResponse("api.iq.text.indexer#query-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        FactFinder factFinder = platform.getFactFinder(finder);
        if (factFinder == null) {
            return new OopsResponse("api.iq.text.indexer#finder-missing", Response.Status.NOT_FOUND).asJSON();
        }
        Repository repository = platform.getRepository(repo);
        if (repository == null) {
            return new OopsResponse("api.iq.text.indexer#repository-missing", Response.Status.NOT_FOUND).asJSON();
        }
        log.info("iq.text.indexer.repository: {} @ {}", repository.isInitialized(), repository.getDataDir().getAbsolutePath());
        if (!repository.isInitialized()) {
            return new OopsResponse("api.iq.text.indexer#repository-offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
        }
        Stopwatch stopwatch = new Stopwatch();
        try (RepositoryConnection connection = repository.getConnection()) {
            IQScriptCatalog library = new IQScriptCatalog(platform.getSelf(), connection);
            String sparql = library.getSPARQL(query);
            if (sparql.isEmpty()) {
                return new OopsResponse("api.iq.text.indexer#query-missing", Response.Status.NO_CONTENT).asJSON();
            }
            log.info("iq.text.indexer.sparql: {}", sparql);
            // SPARQL query used to populate index
            TupleQuery tupleQuery = connection.prepareTupleQuery(RDFPrefixer.getSPARQLPrefix(connection)+sparql);
            long indexed = IndexHelper.index(factFinder, tupleQuery);
            factFinder.save();

            SimpleBindings result = new SimpleBindings();
            result.put("realm", repo);
            result.put("finder", finder);
            result.put("query", query);
            DataResponse response = new DataResponse(result);
            response.set("indexed", indexed);
            response.set("facts", connection.size());
            response.set("elapsed", stopwatch.elapsed());
            return response.asJSON();

        } catch (Exception e) {
            log.error("iq.text.indexer.failed", e);
            return new OopsResponse("api.iq.text.indexer#failed", e).asJSON();
        }
    }

}
