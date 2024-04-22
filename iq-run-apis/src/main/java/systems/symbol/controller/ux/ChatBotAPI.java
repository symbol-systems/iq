package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

import java.io.IOException;

@Path("ux")
public class ChatBotAPI extends GuardedAPI {

    @Path("chat/{topic}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response chat(@HeaderParam("Authorization") String bearer, @PathParam("topic") String topic) throws SecretsException, APIException, IOException {
        DecodedJWT jwt = authenticate(bearer);
        if (jwt==null) {
            return new OopsResponse("api.chat#token-invalid", Response.Status.FORBIDDEN).asJSON();
        }
        if (Validate.isMissing(topic)) {
            return new OopsResponse("api.chat#topic-missing", Response.Status.BAD_REQUEST).asJSON();
        }
        I_Secrets secrets = platform.getSecrets();
        String llmToken = secrets.getSecret("OPENAI_API_KEY");
        if (Validate.isMissing(llmToken)) {
            return new OopsResponse("api.chat#disabled", Response.Status.BAD_REQUEST).asJSON();
        }

        I_LLM<String> llm = new ChatGPT(llmToken, 1000);
        I_Thread<String> chat = new ChatThread();
        I_Thread<String> reply = llm.complete(chat);
//        ScriptAgent agent = new ScriptAgent(IQ_NS.nop, );
        log.info("api.chat: {}", reply.messages());
        return new SimpleResponse(reply).asJSON();
    }
}
