package systems.symbol.controller.search;

import jakarta.ws.rs.*;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.finder.FactFinder;
import systems.symbol.platform.APIPlatform;
import systems.symbol.finder.IndexHelper;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.rdf4j.util.UsefulSPARQL;
import systems.symbol.string.Validate;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

@Path("index")
public class TextIndexer extends GuardedAPI {

    @Inject
    APIPlatform platform;

    @GET
    @Path("{repo}/{finder}/{query: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response importLocal(@PathParam("finder")String finder,
                                @PathParam("repo")String repo, @PathParam("query") String query,
                                @HeaderParam("Authorization") String auth) {
        if (!Validate.isBearer(auth)) {
            log.info("api.iq.text.indexer#protected");
            if (!Validate.isUnGuarded())
                return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
        }
        if (Validate.isNonAlphanumeric(finder)) {
            return new OopsResponse("api.iq.text.indexer#finder-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.iq.text.indexer#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        FactFinder factFinder = platform.getFactFinder(finder);
        if (factFinder == null) {
            return new OopsResponse("api.iq.text.indexer#finder-missing", Response.Status.NOT_FOUND).asJSON();
        }
        Repository repository = platform.getRepository(repo);
        if (repository == null) {
            return new OopsResponse("api.iq.text.indexer#repository-missing", Response.Status.NOT_FOUND).asJSON();
        }
        log.info("api.iq.text.indexer.repository: "+repository.isInitialized()+" @ "+repository.getDataDir().getAbsolutePath());
        if (!repository.isInitialized()) {
            return new OopsResponse("api.iq.text.indexer#repository-offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
        }
        try (RepositoryConnection connection = repository.getConnection()) {
            IQ iq = new IQConnection(platform.getSelf(), connection);
            ScriptCatalog library = new ScriptCatalog(iq);
            String sparql = query==null||query.isEmpty()? RDFPrefixer.getSPARQLPrefix(connection)+UsefulSPARQL.INDEXER :library.getSPARQL(query);
            if (sparql==null || sparql.isEmpty()) {
                return new OopsResponse("api.iq.text.indexer#query-missing", Response.Status.NO_CONTENT).asJSON();
            }
            log.info("sparql.indexer: {}", sparql);
            // SPARQL query used to populate index
            TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);
            long indexed = IndexHelper.index(factFinder, tupleQuery);
            factFinder.save();
            return new SimpleResponse(indexed).asJSON();

        } catch (Exception e) {
            log.error("indexer.failed", e);
            return new OopsResponse("api.iq.text.indexer#failed", e).asJSON();
        }
    }

}
