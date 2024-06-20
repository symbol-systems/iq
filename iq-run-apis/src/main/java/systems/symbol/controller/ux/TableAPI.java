package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the repository and context.
 */
@Path("/ux/table")
@Tag(name = "api.ux.table.name", description = "api.ux.table.description")
public class TableAPI extends GuardedAPI {
    /**
     * Executes a SPARQL query on a specified RDF repository.
     *
     * @param repo       The name of the RDF repository.
     * @param query      The SPARQL query.
     * @param maxResults The maximum number of results to retrieve.
     * @return Response containing the results of the SPARQL query in JSON format.
     */
    @GET
    @Operation(
            summary = "api.ux.table.get.summary",
            description = "api.ux.table.get.description"
    )
    @Path("{repo}/{query: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(
            @PathParam("repo") String repo,
            @PathParam("query") String query,
            @Context UriInfo uriInfo,
            @QueryParam("maxResults") int maxResults,
            @HeaderParam("Authorization") String auth) throws IOException {
        if (!Validate.isBearer(auth)) {
            log.info("ux.table#protected");
            return new OopsResponse("api.select#unauthorized", Response.Status.UNAUTHORIZED).asJSON();
        }
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.ux.table#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (!Validate.isURN(query)) {
            query = platform.getSelf()+query;
        }

        if (maxResults<0) maxResults = 100;
        Repository repository = platform.getRepository(repo);
        if (repository == null) {
            // Return an error response if the repository is not found
            return new OopsResponse("api.ux.table#repository-missing", Response.Status.NOT_FOUND).asJSON();
        }

        try (RepositoryConnection connection = repository.getConnection()) {
            IRI self = Values.iri(jwt.getSubject());
            IQScriptCatalog catalog = new IQScriptCatalog(self, connection);
            Bindings params = MyFacade.bind(uriInfo.getQueryParameters(true));
            Bindings my = MyFacade.rebind(self, params, jwt);
            log.info("ux.table.bind: {}", my.keySet());
            MyFacade.dump(my, System.out);

            String sparql = RDFPrefixer.toSPARQL(connection, catalog.getSPARQL(query, my));
            if (Validate.isMissing(sparql)) {
                return new OopsResponse("api.ux.table#query-missing", Response.Status.NOT_FOUND).asJSON();
            }
            log.info("ux.table.query: {} --> {}", query, sparql);
            TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);

            try (TupleQueryResult result = tupleQuery.evaluate()) {
                List<Map<String, Object>> models = SPARQLMapper.toMaps(result);
                DataResponse response = new DataResponse(models);
                List<String> columns = new ArrayList<>();
                response.set("columns", columns);
                columns.addAll(result.getBindingNames());
                return response.asJSON();
            } catch (QueryEvaluationException e) {
                return new OopsResponse("api.ux.table#query-failed", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
            }
        }
    }
}
