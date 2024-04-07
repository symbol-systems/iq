package systems.symbol.controllers.ux;

import systems.symbol.agent.LLMAgent;
import systems.symbol.agent.ScriptAgent;
import systems.symbol.agent.apis.APIException;
import systems.symbol.controllers.platform.GuardedAPI;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.rdf4j.iq.RepoModel;
import systems.symbol.responses.OopsResponse;
import systems.symbol.responses.SimpleResponse;
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
        com.auth0.jwt.interfaces.DecodedJWT jwt = authenticate(bearer);
        if (jwt==null) {
            return new OopsResponse("api.chat.#token-invalid", Response.Status.FORBIDDEN).asJSON();
        }
        if (Validate.isMissing(topic)) {
            return new OopsResponse("api.chat.#topic-missing", Response.Status.BAD_REQUEST).asJSON();
        }

        try (RepositoryConnection connection = platform.getWorkspace().getCurrentRepository().getConnection()) {
            Model model = new RepoModel(connection);

            String token = platform.getSecrets().getSecret("OPENAI_API_KEY", "default");
            ChatGPT chatGPT = new ChatGPT(token, 1000);
            ScriptAgent scriptAgent = new ScriptAgent(model, connection.getValueFactory().createIRI(jwt.getSubject()));
            LLMAgent bot = new LLMAgent(chatGPT, scriptAgent);
            if (!bot.isOnline()) {
                return new OopsResponse("api.chat.#bot-offline", Response.Status.SERVICE_UNAVAILABLE).asJSON();
            }
            I_Thread<String> chat = bot.say("hello");

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
