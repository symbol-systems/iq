package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.*;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.RealmAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.decide.IntentDecision;
import systems.symbol.decide.ChainOfCommand;
import systems.symbol.llm.Conversation;
import systems.symbol.realm.I_Realm;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;

@Tag(name = "api.ux.avatar.name", description = "api.ux.avatar.description")
@Path("ux/avatar")
public class AvatarAPI extends RealmAPI {

@POST
@Operation(summary = "api.ux.avatar.post.summary", description = "api.ux.avatar.post.description")
@Consumes(MediaType.APPLICATION_JSON)
@Produces("application/ld+json")
@Path("{realm}/{agent: .*}")
public Response chat(@PathParam("realm") String _realm, @PathParam("agent") String _agent,
@HeaderParam("Authorization") String auth, Conversation chat) throws Exception, APIException {
log.info("ux.avatar: {} -> {} -> {}", _realm, _agent, chat);
if (Validate.isNonAlphanumeric(_realm))
return new OopsResponse("api.ux.avatar#repository", Response.Status.BAD_REQUEST).build();
if (Validate.isMissing(_agent))
return new OopsResponse("api.ux.avatar#missing", Response.Status.BAD_REQUEST).build();
IRI agent = Values.iri(_agent);
I_Realm realm = platform.getRealm(Values.iri(_realm + ":"));
if (realm == null)
return new OopsResponse("api.ux.avatar.realm", Response.Status.NOT_FOUND).build();
DecodedJWT jwt;
try {
jwt = authenticate(auth, realm);
} catch (OopsException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).build();
}
Repository repository = realm.getRepository();
if (repository == null)
return new OopsResponse("api.ux.avatar#repository", Response.Status.NOT_FOUND).build();

Stopwatch stopwatch = new Stopwatch();
Bindings bindings = new SimpleBindings();
I_Realm myRealm = platform.getRealm(Values.iri(jwt.getSubject()));
log.info("ux.avatar.realm: {} @ {} & {}", agent, realm.getSelf(), myRealm.getSelf());
Repository myRepo = myRealm.getRepository();
try (RepositoryConnection connection = repository.getConnection()) {
try (RepositoryConnection connection2 = myRepo.getConnection()) {
AgentBuilder builder = new AgentBuilder(agent, connection, bindings, realm.getSecrets());
builder.setThoughts(connection2).scripting().remodel().sparql(connection).self(chat);

// SearchDecision search = builder.decision(realm.getFinder(), chat);
IntentDecision intents = builder.intention(chat);
ChainOfCommand control = builder.control(intents);
I_Agent avatar = builder.agent(chat, control, jwt);
log.info("ux.avatar.start: {} @ {}", agent, avatar.getStateMachine().getState());

avatar.start();
log.info("ux.avatar.done: {} x {} facts", avatar.getStateMachine().getState(),
avatar.getThoughts().size());

log.info("ux.avatar.reply: {}\n-> {} @ {}", chat.latest().getRole(), chat.latest().getContent(),
stopwatch);
return new ChatResponse(chat).build();
}
}
}

@Override
public boolean entitled(DecodedJWT jwt, IRI agent) {
return jwt.getAudience().contains(agent.stringValue());
}
}
