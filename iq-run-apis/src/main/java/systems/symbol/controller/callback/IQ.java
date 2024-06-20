package systems.symbol.controller.callback;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.MyFacade;
import systems.symbol.agent.ScriptAgent;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.HealthCheck;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.Platform;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * RESTful endpoint for checking the event status of the platform and RDF repositories.
 */
@Path("webhook")
public class IQ  {
protected final Logger log = LoggerFactory.getLogger(getClass());

@Inject
protected Platform platform;

/**
 * Receives an event callback from a resource, saves to  repository.
 *
 * @param resource The resource of the event.
 * @return response indicating the repository's event status.
 */
@Path("{resource: .*}")
@POST
@Produces(MediaType.APPLICATION_JSON)
public Response webhookPost( @PathParam("resource") String resource,
@HeaderParam("Authorization") String auth) throws IOException {
if (!Validate.isBearer(auth)) {
return new OopsResponse("api.callback.iq#unauthorized", Response.Status.UNAUTHORIZED).asJSON();
}
if (Validate.isNonAlphanumeric(resource)) {
return new OopsResponse("api.callback.iq#resource-missing", Response.Status.BAD_REQUEST).asJSON();
}

// Get the RDF repository instance from the platform
Repository repository = platform.getWorkspace().getCurrentRepository();
if (repository == null) {
return new OopsResponse("api.callback.iq#repository-missing", Response.Status.NOT_FOUND).asJSON();
}

// Check if the repository is initialized
if (!repository.isInitialized()) {
return new OopsResponse("api.callback.iq#repository.offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}

try (RepositoryConnection connection = repository.getConnection()) {
IQConnection iq = new IQConnection(platform.getSelf(), connection);
IQScriptCatalog library = new IQScriptCatalog(iq);

Literal script = library.getContent(Values.iri(resource), null);
log.info("callback.iq: {}", script);
}
return new HealthCheck("api.event#todo").asJSON();
}

/**
 * Receives an event callback from a resource, saves to  repository.
 *
 * @param resource The resource of the event.
 * @return response indicating the repository's event status.
 */
@Path("{repo}/{resource: .*}")
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response webhookGet( @PathParam("repo") String repo, @PathParam("resource") String resource
,@Context UriInfo uriInfo
) throws Exception {
//log.info("callback.iq.query: {}", uriInfo.getQueryParameters());
if (Validate.isMissing(resource)) {
return new OopsResponse("api.callback.iq#resource-missing", Response.Status.BAD_REQUEST).asJSON();
}

// Get the RDF repository instance from the platform
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.callback.iq#repository-missing", Response.Status.NOT_FOUND).asJSON();
}

// Check if the repository is initialized
if (!repository.isInitialized()) {
return new OopsResponse("api.callback.iq#repository.offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}

try (RepositoryConnection connection = repository.getConnection()) {

IRI self = Values.iri(resource);
LiveModel model = new LiveModel(connection);

ScriptAgent agent = new ScriptAgent(self, model);
Resource state = agent.getStateMachine().getState();
log.info("callback.iq: {} -> {}", state, self);
//RDFDump.dump(model, System.out, RDFFormat.TURTLE);
if (state == null) {
return new OopsResponse("api.callback.unknown", 404).asJSON();
}
Bindings my = new SimpleBindings();
my.put("param", MyFacade.toMap(uriInfo));
MyFacade.dump(my, System.out);

Set<IRI> executed = agent.execute(self, state, my);
state = agent.getStateMachine().getState();
log.info("callback.iq.done: {} -> {}", state, executed);
//MyFacade.dump(my, System.out);
}

return new HealthCheck("api.callback.iq#todo").asJSON();
}

//
//@GET
//@Path("/query")
//@Produces("application/json")
//public Response getQueryParams(@Context MultivaluedMap<String, String> queryParams) {
//// Retrieve all values for the parameter "exampleParam"
//List<String> values = queryParams.get("exampleParam");
//
//// If there are multiple values, they will be in the list
//if (values != null) {
//for (String value : values) {
//System.out.println("Value: " + value);
//}
//}
//
//return Response.ok("Processed query parameters").build();
//}
}
