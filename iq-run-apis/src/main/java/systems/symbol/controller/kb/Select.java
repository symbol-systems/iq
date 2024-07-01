package systems.symbol.controller.kb;

import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the repository and context.
 */
@Path("select")
public class Select extends GuardedAPI {
    /**
     * Executes a SPARQL query on a specified RDF repository.
     *
     * @param repo       The name of the RDF repository.
     * @param query      The SPARQL query.
     * @param maxResults The maximum number of results to retrieve.
     * @return Response containing the results of the SPARQL query in JSON format.
     */
    @GET
    @Path("{repo}/{query: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(
            @PathParam("repo") String repo,
            @PathParam("query") String query,
            @QueryParam("maxResults") int maxResults,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        if (!Validate.isBearer(auth)) {
            log.info("iq.select#protected");
            return new OopsResponse("api.select.unauthorized", Response.Status.UNAUTHORIZED).asJSON();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.iq.select.repository", Response.Status.BAD_REQUEST).asJSON();
        }
        if (Validate.isMissing(query)) {
            return new OopsResponse("api.iq.select.query-invalid", Response.Status.BAD_REQUEST).asJSON();
        }

        if (maxResults<0) maxResults = 10000;
        I_Realm realm = platform.getRealm(repo);
        if (realm==null) return new OopsResponse("api.select.realm", Response.Status.NOT_FOUND).asJSON();
        Repository repository = realm.getRepository();
        if (repository == null) return new OopsResponse("api.select.repository", Response.Status.NOT_FOUND).asJSON();

        try (RepositoryConnection connection = repository.getConnection()) {
            IQConnection iq = new IQConnection(realm.getSelf(), connection);
            IQScriptCatalog library = new IQScriptCatalog(iq);
            String sparql = library.getSPARQL(query);

            if (sparql == null || sparql.isEmpty()) {
                return new OopsResponse("api.iq.select.query-missing", Response.Status.NOT_FOUND).asJSON();
            }
            TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);
            try (TupleQueryResult result = tupleQuery.evaluate()) {
                List<Map<String, Object>> models = SPARQLMapper.toMaps(result);
                return new DataResponse(models).asJSON();
            } catch (QueryEvaluationException e) {
                return new OopsResponse("api.iq.select#query-failed", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
            }
        }
    }
}
