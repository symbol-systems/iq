package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.gpt.CommonLLM;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import java.io.IOException;

/**
 * REST-ful API for Large Language Model (LLM) endpoints.
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
@Path("{repo}/{actor:.*}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public Response chat(@PathParam("repo") String _realm, @PathParam("actor") String actor, @HeaderParam("Authorization") String auth,
   Conversation chat) throws APIException, IOException, SecretsException {
Stopwatch stopwatch = new Stopwatch();
I_Realm realm = platform.getRealm(_realm);
DecodedJWT jwt;
try { jwt = authenticate(auth,realm); } catch (OopsException e) { return new OopsResponse(e.getMessage(), e.getStatus()).asJSON(); }
if (realm==null) return new OopsResponse("api.ux.data.realm", Response.Status.NOT_FOUND).asJSON();
log.info("authenticated: {} -> {} => {}", jwt.getSubject(), jwt.getAudience(), jwt.getClaims());

log.info("ux.chat: {}", chat.messages());
if (chat.messages().isEmpty())
chat.system("hello");

I_Secrets secrets = realm.getSecrets();
String token = secrets.getSecret("MY_GROQ_API_KEY");
if (Validate.isMissing(token)) {
return new OopsResponse("api.ux.chat#disabled", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}
GenericGPT gpt = new GenericGPT(token, CommonLLM.GROQ_Llama3_7b(1000));
gpt.complete(chat);

log.info("ux.chat.reply: {} @ {}", chat.messages(), stopwatch.toString());
return new ChatResponse(chat).asJSON();
}
}
