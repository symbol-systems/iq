package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.Avatar;
import systems.symbol.agent.I_Agent;
import systems.symbol.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.PlatformException;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyStrings;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;

/**
 * REST-ful API to chat with an LLM avatar.
 */
@Tag(name = "api.ux.chat.name", description = "api.ux.chat.description")
@Path("ux/chat")
public class ChatAPI extends GuardedAPI {
@ConfigProperty(name = "iq.realm.chat.minScore", defaultValue = "0.2")
double minScore;
@ConfigProperty(name = "iq.realm.chat.maxResults", defaultValue = "5")
int maxResults;

/**
 * Endpoint for answering queries using LLM Language Models.
 * @return JSON response containing language model results.
 */
@POST
@Operation(summary = "api.ux.chat.post.summary", description = "api.ux.chat.post.description")
@Path("{realm}/{actor:.*}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public Response chat(@PathParam("realm") String _realm, @PathParam("actor") String _actor,
@HeaderParam("Authorization") String auth,
@Context UriInfo uriInfo, @QueryParam("focus") String _focus,
@QueryParam("debug") boolean _debug,
Conversation chat) throws APIException, IOException, SecretsException, PlatformException {
try {
return executeChat(_realm, _actor, auth, chat).build();
} catch (OopsException e) {
log.info("ux.chat.token.oops: {} -> {} ==> {}", _actor, auth, e.getMessage());
return new OopsResponse(e.getMessage(), e.getStatus()).build();
} catch (StateException e) {
log.error("ux.chat.oops: {}", e.getMessage());
return new OopsResponse("ux.chat.state", Response.Status.BAD_REQUEST).build();
} catch (Exception e) {
log.error("ux.chat.fatal: {}", e.getMessage(), e);
return new OopsResponse("ux.chat.error", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

@POST
@Path("{realm}/{actor}/stream")
@Produces("text/event-stream")
@Consumes(MediaType.APPLICATION_JSON)
public Response chatStream(@PathParam("realm") String _realm, @PathParam("actor") String _actor,
@HeaderParam("Authorization") String auth,
@Context UriInfo uriInfo, @QueryParam("focus") String _focus,
@QueryParam("debug") boolean _debug,
Conversation chat) throws APIException, IOException, SecretsException, PlatformException {
try {
ChatResponse resp = executeChat(_realm, _actor, auth, chat);
StreamingOutput stream = output -> {
for (I_LLMessage<String> message : resp.messages) {
String event = "data: {\"role\":\"" + message.getRole() + "\",\"content\":\""
+ message.getContent().replace("\"", "\\\"") + "\"}\n\n";
output.write(event.getBytes(StandardCharsets.UTF_8));
output.flush();
}
};
return Response.ok(stream).type("text/event-stream").build();
} catch (OopsException e) {
log.info("ux.chat.token.oops: {} -> {} ==> {}", _actor, auth, e.getMessage());
return new OopsResponse(e.getMessage(), e.getStatus()).build();
} catch (StateException e) {
log.error("ux.chat.oops: {}", e.getMessage());
return new OopsResponse("ux.chat.state", Response.Status.BAD_REQUEST).build();
} catch (Exception e) {
log.error("ux.chat.fatal: {}", e.getMessage(), e);
return new OopsResponse("ux.chat.error", Response.Status.INTERNAL_SERVER_ERROR).build();
}
}

private ChatResponse executeChat(String _realm, String _actor, String auth, Conversation chat)
throws APIException, IOException, SecretsException, PlatformException, OopsException, StateException, Exception {
Stopwatch stopwatch = new Stopwatch();
log.info("ux.chat: {}", chat.messages());
if (Validate.isNonAlphanumeric(_realm))
throw new OopsException("ux.chat.repository", Response.Status.BAD_REQUEST);
if (Validate.isMissing(_actor))
throw new OopsException("ux.chat.missing", Response.Status.BAD_REQUEST);
IRI actor = Values.iri(_actor);
I_Realm realm = platform.getRealm(Values.iri(_realm + ":"));
if (realm == null)
throw new OopsException("ux.chat.realm.missing", Response.Status.NOT_FOUND);

DecodedJWT jwt = authenticate(auth, realm);

Repository realmRepository = realm.getRepository();
if (realmRepository == null)
throw new OopsException("ux.chat.repository.missing", Response.Status.NOT_FOUND);

IRI user = Values.iri(jwt.getSubject());
I_Realm myRealm = platform.getRealm(user);
if (myRealm == null)
throw new OopsException("ux.chat.realm.missing", Response.Status.NOT_FOUND);

Bindings bindings = new SimpleBindings();
bindings.putAll(chat.getBindings());

try (RepositoryConnection connection = realmRepository.getConnection()) {
log.info("ux.chat.with: {} & {} == {} --> {}", actor, user, myRealm != null,
PrettyStrings.pretty(chat.getBindings()));
log.info("ux.chat.realm: {} @ {} & {} -> {}", actor, realm.getSelf(), myRealm.getSelf(), stopwatch);
AgentBuilder builder = new AgentBuilder(actor, connection, bindings, realm.getSecrets());

builder.jwt(jwt).setThoughts(myRealm.getModel()).realm(myRealm);
Avatar avatar = builder.avatar(chat);
builder.scripting(avatar).sparql(connection);
log.info("ux.chat.ready: {} @ {}", avatar.getSelf(), avatar.getStateMachine().getState());

avatar.start();
avatar.stop();
bindings.put("usage", avatar.getUsage());
bindings.remove("chat");

log.info("ux.chat.reply: {} = {} @ {}", avatar.getThoughts().size(), chat.messages.getLast(), stopwatch);
return new ChatResponse(chat, avatar, bindings);
}
}

}
