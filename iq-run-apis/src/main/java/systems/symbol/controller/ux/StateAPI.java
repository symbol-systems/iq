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
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.Facades;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.net.URI;

@Path("/ux/state")
@Tag(name = "api.ux.update.name", description = "api.ux.update.description")
public class StateAPI extends GuardedAPI {

@GET
@Operation(summary = "api.ux.state.get.summary", description = "api.ux.state.get.description")
@Produces("application/ld+json")
@Path("{realm}/{thing: .*}")
public Response state(@PathParam("realm") String _realm, @PathParam("thing") String _agent,
@HeaderParam("Authorization") String auth) throws Exception {
if (!Validate.isBearer(auth))
return new OopsResponse("ux.state.unauthorized", Response.Status.UNAUTHORIZED).build();
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.state.realm", Response.Status.NOT_FOUND).build();
try {
authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}

if (!Validate.isURN(_agent))
return new OopsResponse("ux.state#invalid", Response.Status.BAD_REQUEST).build();

Stopwatch stopwatch = new Stopwatch();
log.info("ux.state.agent: {}", _agent);
IRI thing = Values.iri(_agent);
Repository repository = realm.getRepository();
if (repository == null)
return new OopsResponse("ux.state.repository", Response.Status.NOT_FOUND).build();

try (RepositoryConnection connection = repository.getConnection()) {
GraphQuery graphQuery = RDFPrefixer.describe(connection, thing);
BindingsResponse response = new BindingsResponse(_agent, graphQuery);
log.info("ux.state.done: {} -> {} -> {} @ {}", _realm, _agent, response.bindings, stopwatch);
return response.build();
}
}

/**
 * Executes a SPARQL update query on a specified RDF repository.
 *
 * @param _realm The name of the RDF repository.
 * @param query  The query of a SPARQL construct query.
 * @return Response containing the results of the SPARQL query in JSON format.
 */
@POST
@Operation(summary = "api.ux.update.post.summary", description = "api.ux.update.post.description")
@Path("{realm}/{query: .*}")
@Produces("application/ld+json")
public Response update(@PathParam("realm") String _realm,
@PathParam("query") String query,
SimpleBindings state,
@Context UriInfo uriInfo,
@HeaderParam("Authorization") String auth) throws IOException, SecretsException {
log.info("ux.update: {} --> {} -> {}", _realm, query, state.keySet());

if (Validate.isNonAlphanumeric(_realm))
return new OopsResponse("ux.update.realm", Response.Status.BAD_REQUEST).build();
I_Realm realm = platform.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.update.realm", Response.Status.NOT_FOUND).build();
DecodedJWT jwt;
try {
jwt = authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}
if (!Validate.isURN(query))
return new OopsResponse("ux.update.query", Response.Status.BAD_REQUEST).build();

log.info("ux.update.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

Repository repository = realm.getRepository();
if (repository == null)
return new OopsResponse("ux.update.repository", Response.Status.NOT_FOUND).build();
IRI self = Values.iri(jwt.getSubject());
try (RepositoryConnection connection = repository.getConnection()) {
IQScriptCatalog catalog = new IQScriptCatalog(realm.getSelf(), connection);
Bindings params = Facades.rebind(self, state);
Bindings my = Facades.rebind(self, params, jwt);
log.info("ux.update.bind: {}", my.keySet());

String sparql = RDFPrefixer.toSPARQL(connection, catalog.getSPARQL(query, my));
if (Validate.isMissing(sparql))
return new OopsResponse("ux.update#query-missing", Response.Status.NOT_FOUND).build();

log.info("ux.update.query: {} --> {}", query, sparql);
Update graphQuery = connection.prepareUpdate(sparql);
graphQuery.execute();

URI redirect = uriInfo.getBaseUriBuilder().path("/ux/state/" + _realm + "/" + self.stringValue()).build();
log.info("ux.update.redirect: {} -> {}", self, redirect);
return Response.created(redirect).build();
}
}
}
