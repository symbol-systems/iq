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
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.decide.ChainOfCommand;
import systems.symbol.decide.IntentDecision;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.prompt.AgentPrompt;
import systems.symbol.prompt.PromptChain;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.PlatformException;
import systems.symbol.secrets.SecretsException;
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

/**
 * Endpoint for answering queries using LLM Language Models.
 *
 * @return JSON response containing language model results.
 */
@POST
@Operation(
summary = "api.ux.chat.post.summary",
description = "api.ux.chat.post.description"
)
@Path("{realm}/{actor:.*}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public Response chat(@PathParam("realm") String _realm, @PathParam("actor") String _actor, @HeaderParam("Authorization") String auth,
   Conversation chat) throws APIException, IOException, SecretsException, PlatformException {
Stopwatch stopwatch = new Stopwatch();
log.info("ux.chat: {}", chat.messages());
if (chat.messages().isEmpty()) return new OopsResponse("api.ux.chat.empty", Response.Status.NOT_FOUND).asJSON();
if (Validate.isNonAlphanumeric(_realm)) return new OopsResponse("api.ux.chat.repository", Response.Status.BAD_REQUEST).asJSON();
if (Validate.isMissing(_actor)) return new OopsResponse("api.ux.chat.missing", Response.Status.BAD_REQUEST).asJSON();
IRI actor = Values.iri(_actor);
I_Realm realm = platform.getRealm(Values.iri(_realm+":"));
if (realm==null) return new OopsResponse("api.ux.chat.realm.missing", Response.Status.NOT_FOUND).asJSON();
DecodedJWT jwt;
try { jwt = authenticate(auth, realm); } catch (OopsException e) {log.info("ux.chat.token");return new OopsResponse(e.getMessage(), e.getStatus()).asJSON(); }
Repository realmRepository = realm.getRepository();
if (realmRepository == null) return new OopsResponse("api.ux.chat.repository.missing", Response.Status.NOT_FOUND).asJSON();

Bindings bindings = new SimpleBindings();
IRI user = Values.iri(jwt.getSubject());

try (RepositoryConnection connection = realmRepository.getConnection()) {
I_Realm myRealm = platform.getRealm(user);
log.info("ux.chat.with: {} & {} == {}", actor, user, myRealm!=null);
if (myRealm==null) return new OopsResponse("api.ux.chat.realm.missing", Response.Status.NOT_FOUND).asJSON();
log.info("ux.chat.realm: {} @ {} & {} -> {}", actor, realm.getSelf(), myRealm.getSelf(), stopwatch);
AgentBuilder builder = new AgentBuilder(actor, connection, bindings, realm.getSecrets()).scripting();
builder.jwt(jwt);
bindings.put("realm", _realm);
bindings.put("capacity", connection.size());
I_Agent agent = builder.avatar(chat);
agent.start();
agent.stop();
log.info("ux.chat.reply: {} = {} @ {}", agent.getThoughts().size(),chat.messages.getLast(), stopwatch);
return new ChatResponse(chat).asJSON();
} catch (StateException e) {
log.error("ux.chat.oops: {}", e.getMessage());
return new OopsResponse("api.ux.chat.state", Response.Status.BAD_REQUEST).asJSON();
} catch (Exception e) {
log.error("ux.chat.fatal: {}", e.getMessage(), e);
return new OopsResponse("api.ux.chat.oops", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
}
}
}
