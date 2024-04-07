package systems.symbol.controllers.llm;

import systems.symbol.agent.apis.APIException;
import systems.symbol.platform.APIPlatform;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.responses.OopsResponse;
import systems.symbol.responses.SimpleResponse;
import systems.symbol.string.Validate;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

/**
 * REST-ful API for Large Language Model (LLM) endpoints.
 */
@Path("openai")
public class OpenAI {
public static final String COMPLETIONS_API = "https://api.openai.com/v1/chat/completions";
private static final Object SEED = 0;
@Inject
APIPlatform platform;
ObjectMapper objectMapper = new ObjectMapper();

/**
 * Endpoint for answering queries using OpenAI Language Models.
 *
 * @param model  The language model to use.
 * @param repo   The repository to fetch data from.
 * @param prompt The prompt for the language model.
 * @param query  Additional query parameters.
 * @param maxTokens  Maximum number of tokens for the response.
 * @param auth   Authorization token in Bearer format.
 * @return   JSON response containing language model results.
 */
@GET
@Path("{model}/{repo}/{prompt: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response answer(@PathParam("model") String model,
   @PathParam("repo") String repo,
   @PathParam("prompt") String prompt,
   @QueryParam("query") String query,
   @QueryParam("maxTokens") int maxTokens,
   @HeaderParam("Authorization") String auth
) throws APIException, IOException {

// Check for valid Bearer token in the Authorization header
if (!Validate.isBearer(auth)) {
//return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}
String TOKEN = System.getenv("OPENAI_API_KEY");
if (TOKEN==null|TOKEN.isEmpty()) {
return new OopsResponse("api.llm.oai#not-configured", Response.Status.SERVICE_UNAVAILABLE).asJSON();
}
ChatGPT gpt = new ChatGPT(System.getenv("OPENAI_API_KEY"), 100);
ChatThread chat = new ChatThread();
chat.add("me", "user", query);
I_Thread generated = gpt.generate(chat);

System.out.println("api.llm.openai.generated: "+generated.messages());
// to BODY into `JSON`
//Map<String, Object> map = objectMapper.readValue(body, Map.class);

return new SimpleResponse(generated).asJSON();
}
}
