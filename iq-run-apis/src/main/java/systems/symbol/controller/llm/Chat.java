package systems.symbol.controller.llm;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.gpt.CommonLLM;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;

import java.io.IOException;

/**
 * REST-ful API for Large Language Model (LLM) endpoints.
 */
@Path("llm")
public class Chat extends GuardedAPI {
    /**
     * CORS pre-flight
     */
    @OPTIONS
    @Path("chat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preflight() {
        return new SimpleResponse("").asJSON();
    }

    /**
     * Endpoint for answering queries using LLM Language Models.
     *
     * @param auth       Authorization token in Bearer format.
     * @return           JSON response containing language model results.
     */
    @POST
    @Path("chat")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response simpleChat(@HeaderParam("Authorization") String auth,
                           Conversation chat
                           ) throws APIException, IOException {
        I_Secrets secrets = platform.getSecrets();
        String token = secrets.getSecret("GROQ_API_KEY");
        if (Validate.isMissing(token)) {
            return new OopsResponse("api.llm.chat#disabled", Response.Status.SERVICE_UNAVAILABLE).asJSON();
        }
        if (!Validate.isBearer(auth)) {
            log.info("api.llm.chat#protected");
return new OopsResponse("api.llm.chat#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
        }
        DecodedJWT jwt = authenticate(auth);
        if (jwt == null) {
            return new OopsResponse("api.llm.chat#jwt-invalid", Response.Status.FORBIDDEN).asJSON();
        }
        log.info("authenticated: {} -> {} => {}", jwt.getSubject(), jwt.getAudience(), jwt.getClaims());

        log.info("api.llm.chat: {}", chat.messages());
        if (chat.messages().isEmpty())
            chat.system("hello");

        GenericGPT gpt = new GenericGPT(token, CommonLLM.newGROQ_Llama3_7b(1000));
        gpt.complete(chat);

        log.info("api.llm.chat.reply: {}", chat.messages());
        return new ChatResponse(chat).asJSON();
    }
}
