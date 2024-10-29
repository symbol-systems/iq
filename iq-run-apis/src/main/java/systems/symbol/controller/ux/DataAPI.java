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
import systems.symbol.agent.Facades;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the
 * repository and context.
 */
@Path("/ux/data")
@Tag(name = "api.ux.data.name", description = "api.ux.data.description")
public class DataAPI extends GuardedAPI {
    /**
     * Executes a SPARQL query on a specified RDF repository.
     *
     * @param _realm     The name of the RDF repository.
     * @param query      The SPARQL query.
     * @param maxResults The maximum number of results to retrieve.
     * @return Response containing the results of the SPARQL query in JSON format.
     */
    @GET
    @Operation(summary = "api.ux.data.get.summary", description = "api.ux.data.get.description")
    @Path("{realm}/{query: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(
            @PathParam("realm") String _realm,
            @PathParam("query") String query,
            @Context UriInfo uriInfo,
            @QueryParam("maxResults") int maxResults,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        if (!Validate.isBearer(auth)) {
            log.info("ux.data.protected");
            return new OopsResponse("ux.ux.data.unauthorized", Response.Status.UNAUTHORIZED).build();
        }
        if (Validate.isNonAlphanumeric(_realm)) {
            return new OopsResponse("ux.ux.data.repository", Response.Status.BAD_REQUEST).build();
        }
        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("ux.ux.data.realm", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }

        if (!Validate.isURN(query)) {
            query = realm.getSelf() + query;
        }

        if (maxResults < 0)
            maxResults = 100;
        Repository repository = realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.ux.data.repository", Response.Status.NOT_FOUND).build();

        try (RepositoryConnection connection = repository.getConnection()) {
            IRI self = Values.iri(jwt.getSubject());
            IQScriptCatalog catalog = new IQScriptCatalog(self, connection);
            Bindings params = Facades.bind(uriInfo.getQueryParameters(true));
            Bindings my = Facades.rebind(self, params, jwt);
            log.info("ux.data.bind: {}", my.keySet());
            Facades.dump(my, System.out);

            String sparql = RDFPrefixer.toSPARQL(connection, catalog.getSPARQL(query, my));
            if (Validate.isMissing(sparql)) {
                return new OopsResponse("ux.ux.data.query", Response.Status.NOT_FOUND).build();
            }
            log.info("ux.data.sparql: {} --> {}", query, sparql);
            TupleQuery tupleQuery = connection.prepareTupleQuery(sparql);

            try (TupleQueryResult result = tupleQuery.evaluate()) {
                List<Map<String, Object>> models = SPARQLMapper.toMaps(result);
                DataResponse response = new DataResponse(models);
                List<String> columns = new ArrayList<>();
                response.set("columns", columns);
                columns.addAll(result.getBindingNames());
                return response.build();
            } catch (QueryEvaluationException e) {
                return new OopsResponse("ux.ux.data.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
