package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.LLMAgent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.aspects.AgentAspects;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_Thread;
import systems.symbol.rdf4j.iq.LiveModel;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.IOException;

@Path("ux")
public class ChatBotAPI extends GuardedAPI {

@Path("chat/{topic}")
@GET
@Produces(MediaType.APPLICATION_JSON)
public Response chat(@HeaderParam("Authorization") String bearer, @PathParam("topic") String topic) {
DecodedJWT jwt = authenticate(bearer);
if (jwt==null) {
return new OopsResponse("api.chat.#token-invalid", Response.Status.FORBIDDEN).asJSON();
}
if (Validate.isMissing(topic)) {
return new OopsResponse("api.chat.#topic-missing", Response.Status.BAD_REQUEST).asJSON();
}

try (RepositoryConnection connection = platform.getWorkspace().getCurrentRepository().getConnection()) {
Model model = new LiveModel(connection);
LLMAgent bot = AgentAspects.openai(Values.iri(jwt.getSubject()), model, platform.getSecrets(), 1000);
if (!bot.isOnline()) {
return new OopsResponse("api.chat.#bot-offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}
I_Thread<String> chat = bot.prompt("hello");

log.info("api.chat: {}", chat.messages());
return new SimpleResponse(chat).asJSON();
} catch (APIException e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.chat.#api-error", Response.Status.SERVICE_UNAVAILABLE).asJSON();
} catch (IOException e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.chat.#io-error", Response.Status.SERVICE_UNAVAILABLE).asJSON();
} catch (SecretsException e) {
log.error("secrets.failed: {}", e.getMessage(), e);
throw new RuntimeException(e);
} catch (StateException e) {
log.error("state.failed: {}", e.getMessage(), e);
throw new RuntimeException(e);
}

}
}
