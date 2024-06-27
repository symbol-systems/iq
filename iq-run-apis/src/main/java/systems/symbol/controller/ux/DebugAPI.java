package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.Agentic;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.Prompts;
import systems.symbol.platform.AgentService;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.string.Validate;

import javax.script.Bindings;
import javax.script.SimpleBindings;

@Tag(name = "api.ux.debug.name", description = "api.ux.debug.description")
@Path("ux/debug")
public class DebugAPI extends GuardedAPI {

@GET
@Operation(
summary = "api.ux.debug.post.summary",
description = "api.ux.debug.post.description"
)
@Consumes(MediaType.APPLICATION_JSON)
@Produces("application/ld+json")
@Path("{repo}/{focus: .*}")
public Response ask(@PathParam("repo") String repo, @PathParam("focus") String _focus, @HeaderParam("Authorization") String auth) throws Exception {
DecodedJWT jwt;
try {
jwt = authenticate(auth);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
if (!Validate.isURN(_focus)) {
return new OopsResponse("api.ux.debug.invalid", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isNonAlphanumeric(repo)) {
return new OopsResponse("api.ux.debug.repository-invalid", Response.Status.BAD_REQUEST).asJSON();
}
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.ux.debug.repository-missing", Response.Status.NOT_FOUND).asJSON();
}

try (RepositoryConnection connection = repository.getConnection()) {
IRI focus = Values.iri(_focus);
Bindings my = MyFacade.rebind(focus, new SimpleBindings(), jwt);

LiveModel model = new LiveModel(connection);
Agentic<String, Resource> agentic = new Agentic<>(()->focus, my, new Conversation());
AgentService service = new AgentService(focus, connection, null, my);
I_Assist<String> decisions = Prompts.decision(service.getAgent(), agentic);
my.put("decisions", decisions);
I_Assist<String> chats = Prompts.prompt(focus, service.getAgent().getStateMachine().getState(), model, my);
my.put("chats", chats);
SimpleBindings claims = new SimpleBindings();
my.put("claims", claims);
claims.put("name", jwt.getClaim("name").asString());
claims.put("aud", jwt.getClaim("aud").asArray(String.class));
claims.put("roles", jwt.getClaim("roles").asArray(String.class));

log.info("ux.debug.self: {} -> {}", focus, my);
return new SimpleResponse(my).asJSON();
}
}
}
