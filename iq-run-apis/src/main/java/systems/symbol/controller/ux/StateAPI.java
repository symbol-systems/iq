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
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

@Path("/ux/state")
@Tag(name = "api.ux.update.name", description = "api.ux.update.description")
public class StateAPI extends GuardedAPI {

@GET
@Operation(
summary = "api.ux.agent.get.summary",
description = "api.ux.agent.get.description"
)
@Produces("application/ld+json")
@Path("{repo}/{thing: .*}")
public Response state(@PathParam("repo") String repo,@PathParam("thing") String _agent, @HeaderParam("Authorization") String auth) throws Exception {
if (!Validate.isBearer(auth)) {
log.info("ux.state#protected");
return new OopsResponse("api.ux.state#unauthorized", Response.Status.UNAUTHORIZED).asJSON();
}
DecodedJWT jwt = authenticate(auth);
if (jwt==null) {
return new OopsResponse("api.ux.state#token-invalid", Response.Status.FORBIDDEN).asJSON();
}
I_Secrets secrets = platform.getSecrets();
String llmToken = secrets.getSecret("MY_OPENAI_API_KEY");
if (Validate.isMissing(llmToken)) {
return new OopsResponse("api.ux.state#unavailable", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}

if (!Validate.isURN(_agent)) {
return new OopsResponse("api.ux.state#invalid", Response.Status.BAD_REQUEST).asJSON();
}
Stopwatch stopwatch = new Stopwatch();
log.info("ux.state.agent: {}", _agent);

IRI thing = Values.iri(_agent);
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.ux.state#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
try (RepositoryConnection connection = repository.getConnection()) {
String sparql = RDFPrefixer.toSPARQL(connection, "DESCRIBE <" + thing + ">");
GraphQuery graphQuery = connection.prepareGraphQuery(sparql);
BindingsResponse response = new BindingsResponse(_agent, graphQuery);
log.info("ux.state.done: {} -> {} -> {} @ {}", repo, _agent, response.bindings, stopwatch);
return response.asJSON();
}
}

/**
 * Executes a SPARQL update query on a specified RDF repository.
 *
 * @param repo   The name of the RDF repository.
 * @param query   The query of a SPARQL construct query.
 * @return Response containing the results of the SPARQL query in JSON format.
 */
@POST
@Operation(
summary = "api.ux.update.post.summary",
description = "api.ux.update.post.description"
)
@Path("{repo}/{query: .*}")
@Produces("application/ld+json")
public Response update(@PathParam("repo") String repo,
  @PathParam("query") String query,
  SimpleBindings state,
  @Context UriInfo uriInfo,
  @HeaderParam("Authorization") String auth) throws IOException {
log.info("ux.update: {} --> {} -> {}", repo, query, state.keySet());

DecodedJWT jwt;
try {
jwt = authenticate(auth);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.ux.update#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (!Validate.isURN(query)) {
return new OopsResponse("api.ux.update#query-invalid", Response.Status.BAD_REQUEST).asJSON();
}
log.info("ux.update.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.ux.update#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
IRI self = Values.iri(jwt.getSubject());
try (RepositoryConnection connection = repository.getConnection()) {
IQScriptCatalog catalog = new IQScriptCatalog(platform.getSelf(), connection);
Bindings params = MyFacade.rebind(self, state);
Bindings my = MyFacade.rebind(self, params, jwt);
log.info("ux.update.bind: {}", my.keySet());

String sparql = RDFPrefixer.toSPARQL(connection, catalog.getSPARQL(query, my));
if (Validate.isMissing(sparql)) {
return new OopsResponse("api.ux.update#query-missing", Response.Status.NOT_FOUND).asJSON();
}

log.info("ux.update.query: {} --> {}", query, sparql);
Update graphQuery = connection.prepareUpdate(sparql);
graphQuery.execute();

URI redirect = uriInfo.getBaseUriBuilder().path("/ux/state/"+repo+"/"+self.stringValue()).build();
log.info("ux.update.redirect: {} -> {}", self, redirect);
return Response.created(redirect).build();
}
}
}
