package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.finder.FactFinder;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RESTful endpoint for searching facts in a knowledge base then hydrating the
 * results from the knowledge base.
 */
@Path("ux/find")
@Tag(name = "api.ux.find.name", description = "api.ux.find.description")
public class FindAPI extends GuardedAPI {

    /**
     * Searches for facts in a knowledge base using a specified text finder and
     * SPARQL query.
     * Hydrates the found items using the specified SPARQL query and returns as JSON
     *
     * @param repo       The name of the text finder.
     * @param model      The name of the SPARQL query.
     * @param query      The search query.
     * @param maxResults The maximum number of results to retrieve.
     * @param relevancy  The relevancy threshold for search results.
     * @return Response containing the search results in JSON format.
     */
    @GET
    @Operation(summary = "api.ux.find.get.summary", description = "api.ux.find.get.description")
    @Path("{repo}/{model: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @PathParam("repo") String repo,
            @PathParam("model") String model,
            @QueryParam("query") String query,
            @QueryParam("maxResults") int maxResults,
            @QueryParam("relevancy") double relevancy,
            @Context UriInfo uriInfo,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        return doSearch(repo, model, query, maxResults, relevancy, uriInfo, auth);
    }

    @POST
    @Operation(summary = "api.ux.find.post.summary", description = "api.ux.find.post.description")
    @Path("{repo}/{model: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchModel(
            @PathParam("repo") String repo,
            @PathParam("model") String model,
            @QueryParam("query") String query,
            @QueryParam("maxResults") int maxResults,
            @QueryParam("relevancy") double relevancy,
            @Context UriInfo uriInfo,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        return doSearch(repo, model, query, maxResults, relevancy, uriInfo, auth);
    }

    public Response doSearch(
            String repo,
            String model,
            String query,
            int maxResults,
            double relevancy,
            UriInfo uriInfo,
            String auth) throws SecretsException {

        Stopwatch stopwatch = new Stopwatch();
        log.info("ux.find: {} --> {} -> {}", repo, query, uriInfo.getQueryParameters().keySet());

        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.ux.find#repository", Response.Status.BAD_REQUEST).build();
        }
        I_Realm realm = platform.getRealm(repo);
        if (realm == null)
            return new OopsResponse("api.ux.find.realm", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }

        if (!Validate.isURN(model)) {
            model = model.isEmpty() ? realm.getSelf() + "ux/find" : realm.getSelf() + model;
        }
        log.info("ux.find.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());
        FactFinder searcher = realm.getFinder();
        if (searcher == null) {
            return new OopsResponse("api.ux.find#finder-missing", Response.Status.NOT_FOUND).build();
        }
        Repository repository = realm.getRepository();
        if (repository == null) {
            return new OopsResponse("api.ux.find#repository", Response.Status.NOT_FOUND).build();
        }
        if (!repository.isInitialized()) {
            return new OopsResponse("api.ux.find#repository.offline", Response.Status.SERVICE_UNAVAILABLE).build();
        }

        log.info("timer.start: {}", stopwatch.summary());
        // Use the platform SPARQL repository
        try (RepositoryConnection connection = repository.getConnection()) {
            IQConnection iq = new IQConnection(realm.getSelf(), connection);
            IQScriptCatalog library = new IQScriptCatalog(iq);

            // Set a default relevancy threshold if not provided
            if (relevancy < 0.1)
                relevancy = 0.5;

            // Find matches using the text finder
            List<EmbeddingMatch<TextSegment>> matches = searcher.find(query, maxResults, relevancy);
            log.info("ux.find.matches: {} @ {}", matches.size(), stopwatch.summary());

            StringBuilder theseMatches = new StringBuilder();
            // Convert matches to a VALUES clause for SPARQL query
            for (EmbeddingMatch<TextSegment> match : matches) {
                theseMatches.append("(")
                        .append("<").append(match.embeddingId()).append("> ")
                        .append(match.score())
                        .append(")");
            }
            Map<String, Object> bindings = new HashMap<>();
            bindings.put("these", theseMatches.toString());

            // The query is interpolated to respect {{these}} VALUES bindings
            String hydrateQuery = library.getSPARQL(model, bindings);
            log.info("ux.find.hydrate: {} -> {} @ {}", model, hydrateQuery, stopwatch.summary());
            if (hydrateQuery == null || hydrateQuery.isEmpty()) {
                return new OopsResponse("api.ux.find#hydrate-not-found", Response.Status.NOT_FOUND).build();
            }

            // Use model to synthesize an answer model
            TupleQuery tupleQuery = connection.prepareTupleQuery(hydrateQuery);
            try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
                List<Map<String, Object>> models = SPARQLMapper.toMaps(evaluate);
                log.info("ux.find.done: {} @ {}", models.size(), stopwatch.summary());
                DataResponse response = new DataResponse(models);
                response.set("found", models.size());
                response.set("elapsed", stopwatch.elapsed());
                return response.build();
            } catch (QueryEvaluationException e) {
                return new OopsResponse("api.ux.find#query-failed", Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch (IOException e) {
            return new OopsResponse("api.ux.find#failed", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
