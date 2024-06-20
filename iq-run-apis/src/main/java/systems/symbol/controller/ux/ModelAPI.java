package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.LDResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import java.io.IOException;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the repository and context.
 */
@Path("/ux/model")
@Tag(name = "api.ux.model.name", description = "api.ux.model.description")
public class ModelAPI extends GuardedAPI {

    /**
     * Executes a SPARQL construct query on a specified RDF repository.
     *
     * @param repo       The name of the RDF repository.
     * @param query       The query of a SPARQL construct query.
     * @return Response containing the results of the SPARQL query in JSON format.
     */
    @GET
    @Operation(
            summary = "api.ux.model.post.summary",
            description = "api.ux.model.post.description"
    )
    @Path("{repo}/{query: .*}")
    @Produces("application/ld+json")
    public Response graph(@PathParam("repo") String repo,
                          @PathParam("query") String query,
                          @Context UriInfo uriInfo,
                          @HeaderParam("Authorization") String auth) throws IOException {
        log.info("ux.model: {} --> {} -> {}", repo, query, uriInfo.getQueryParameters().keySet());

        if (!Validate.isBearer(auth)) {
            log.info("ux.model.protected");
            return new OopsResponse("api.ux.model#unauthorized", Response.Status.UNAUTHORIZED).asJSON();
        }
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.ux.model#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (!Validate.isURN(query)) {
            query = query.isEmpty() ?platform.getSelf()+"ux/model":platform.getSelf()+query;
        }
        log.info("ux.model.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

        Repository repository = platform.getRepository(repo);
        if (repository == null) {
            return new OopsResponse("api.ux.model#repository-missing", Response.Status.NOT_FOUND).asJSON();
        }
        try (RepositoryConnection connection = repository.getConnection()) {

            IQConnection iq = new IQConnection(platform.getSelf(), connection);
            IQScriptCatalog catalog = new IQScriptCatalog(iq);
            Bindings params = MyFacade.bind(uriInfo.getQueryParameters(true));
            IRI self = Values.iri(jwt.getSubject());
            Bindings my = MyFacade.rebind(self, params, jwt);
            log.info("ux.model.bind: {}", my.keySet());

            String sparql = RDFPrefixer.toSPARQL(connection, catalog.getSPARQL(query, my));
            if (Validate.isMissing(sparql)) {
                return new OopsResponse("api.ux.model#query-missing", Response.Status.NOT_FOUND).asJSON();
            }
            log.info("ux.model.query: {} -> {} --> {}", repo, query, sparql);
            GraphQuery graphQuery = connection.prepareGraphQuery(sparql);
            LDResponse response = new LDResponse(graphQuery);
            return response.asJSON();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
