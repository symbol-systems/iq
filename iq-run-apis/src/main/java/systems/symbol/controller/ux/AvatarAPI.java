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
import org.eclipse.rdf4j.rio.RDFFormat;
import systems.symbol.agent.*;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.fsm.I_StateMachine;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.Prompts;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.platform.AgentService;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;

@Tag(name = "api.ux.avatar.name", description = "api.ux.avatar.description")
@Path("ux/avatar")
public class AvatarAPI extends GuardedAPI {

@GET
@Operation(
summary = "api.ux.avatar.get.summary",
description = "api.ux.avatar.get.description"
)
@Produces("application/ld+json")
@Path("{repo}/{agent: .*}")
public Response hello(@PathParam("repo") String repo,@PathParam("agent") String _agent, @HeaderParam("Authorization") String auth) throws Exception, APIException {
Stopwatch stopwatch = new Stopwatch();
DecodedJWT jwt;
try {
jwt = authenticate(auth);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
if (!Validate.isURN(_agent)) {
return new OopsResponse("api.ux.hello#invalid", Response.Status.BAD_REQUEST).asJSON();
}

I_Secrets secrets = platform.getSecrets();
String llmToken = secrets.getSecret("MY_OPENAI_API_KEY");
if (Validate.isMissing(llmToken)) {
return new OopsResponse("api.ux.hello#disabled", Response.Status.BAD_REQUEST).asJSON();
}
IRI actor = Values.iri(_agent);
Repository repository = platform.getRepository(repo);
try (RepositoryConnection connection = repository.getConnection()) {
Bindings my = MyFacade.rebind(actor, new SimpleBindings(), jwt);

GenericGPT llm = new GenericGPT(llmToken, 1000);
AgentService service = new AgentService(actor, connection, secrets, my);
Resource state = service.getAgent().getStateMachine().getState();
log.info("ux.hello.state: {} -> {} @ {}", repo, _agent, state);
I_Assist<String> chat = Prompts.prompt(actor, state, new LiveModel(connection), my);
llm.complete(chat);
log.info("ux.hello.reply: {} -> {} @ {}", actor, chat.messages().size(), stopwatch);
return new ChatResponse(chat).asJSON();
}
}

@POST
@Operation(
summary = "api.ux.avatar.post.summary",
description = "api.ux.avatar.post.description"
)
@Consumes(MediaType.APPLICATION_JSON)
@Produces("application/ld+json")
@Path("{repo}/{agent: .*}")
public Response ask(@PathParam("repo") String repo,@PathParam("agent") String _actor, @HeaderParam("Authorization") String auth, Conversation chat) throws Exception, APIException {
Stopwatch stopwatch = new Stopwatch();
DecodedJWT jwt;
try {
jwt = authenticate(auth);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
if (Validate.isMissing(_actor)) {
return new OopsResponse("api.ux.avatar#missing", Response.Status.BAD_REQUEST).asJSON();
}
IRI actor = Values.iri(_actor);
Bindings my = MyFacade.rebind(actor, new SimpleBindings(), jwt);
Repository repository = platform.getRepository(repo);
log.info("ux.avatar.ask: {} -> {} -> {}", repo, _actor, chat);
if (repository == null) {
return new OopsResponse("api.ux.avatar#repository-missing", Response.Status.NOT_FOUND).asJSON();
}
Repository myRepo = platform.getWorkspace().alwaysGetRepository(jwt.getSubject());
try (RepositoryConnection connection = repository.getConnection()) {
try (RepositoryConnection connection2 = myRepo.getConnection()) {
AvatarBuilder builder = new AvatarBuilder(actor, 1000, my, platform.getSecrets());
builder.setGround(connection);
builder.setThoughts(connection2);
builder.executive().remodel().search(platform.getFactFinder(repo)).sparql(connection);
//RDFDump.dump(builder.getThoughts());
I_Agent avatar = builder.build(chat);
log.info("ux.avatar.start: {} @ {}", actor, avatar.getStateMachine().getState());
//RDFDump.dump(avatar.getThoughts());
avatar.start();
log.info("ux.avatar.done: {} x {} facts", avatar.getStateMachine().getState(), avatar.getThoughts().size());
//avatar.memorize(connection2);
log.info("ux.avatar.reply: {}\n-> {} @ {}", chat.latest().getRole(), chat.latest().getContent(), stopwatch);
return new ChatResponse(chat).asJSON();
}
}
}
}
