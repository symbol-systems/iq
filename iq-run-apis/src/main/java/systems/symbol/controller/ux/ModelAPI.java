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
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import java.io.IOException;

/**
 * RESTful endpoint for executing SPARQL queries on RDF repositories.
 * The Queries are stored within the ScriptCatalog associated with the
 * repository and context.
 */
@Path("/ux/model")
@Tag(name = "api.ux.model.name", description = "api.ux.model.description")
public class ModelAPI extends GuardedAPI {

/**
 * Executes a SPARQL construct query on a specified RDF repository.
 *
 * @param _realm The name of the RDF repository.
 * @param query  The query of a SPARQL construct query.
 * @return Response containing the results of the SPARQL query in JSON format.
 */
@GET
@Operation(summary = "api.ux.model.get.summary", description = "api.ux.model.get.description")
@Path("{realm}/{query: .*}")
@Produces("application/ld+json")
public Response graph(@PathParam("realm") String _realm,
@PathParam("query") String query,
@Context UriInfo uriInfo,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {
log.info("ux.model: {} --> {} -> {}", _realm, query, uriInfo.getQueryParameters().keySet());

if (!Validate.isBearer(auth))
return new OopsResponse("api.ux.model.unauthorized", Response.Status.UNAUTHORIZED).asJSON();
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("api.ux.model.realm", Response.Status.NOT_FOUND).asJSON();
DecodedJWT jwt;
try {
jwt = authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}

if (!Validate.isURN(query)) {
query = query.isEmpty() ? realm.getSelf() + "ux/model" : realm.getSelf() + query;
}
log.info("ux.model.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

Repository repository = realm.getRepository();
if (repository == null)
return new OopsResponse("api.ux.model.repository", Response.Status.NOT_FOUND).asJSON();

IRI self = Values.iri(jwt.getSubject());
try (RepositoryConnection connection = repository.getConnection()) {
IQConnection iq = new IQConnection(realm.getSelf(), connection);
IQScriptCatalog catalog = new IQScriptCatalog(iq);
Bindings params = MyFacade.bind(uriInfo.getQueryParameters(true));
Bindings my = MyFacade.rebind(self, params, jwt);

String sparql = RDFPrefixer.toSPARQL(connection, catalog.getSPARQL(query, my));
log.info("ux.mind.sparql: {} -> {}", my.keySet(), sparql);
if (Validate.isMissing(sparql)) {
return new OopsResponse("api.ux.model.query-missing", Response.Status.NOT_FOUND).asJSON();
}
GraphQuery graphQuery = connection.prepareGraphQuery(sparql);
LDResponse response = new LDResponse(graphQuery);
return response.asJSON();
} catch (Exception e) {
log.error("ux.mind.failed: {} -> {} ==> {}", self, query, e.getMessage());
return new OopsResponse("api.ux.model.failed", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}
}
}
