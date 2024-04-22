package systems.symbol.controller.llm;

import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.string.Validate;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

/**
 * REST-ful API for Large Language Model (LLM) endpoints.
 */
@Path("openai")
public class OpenAI extends GuardedAPI {
    /**
     * Endpoint for answering queries using OpenAI Language Models.
     *
     * @param model      The language model to use.
     * @param repo       The repository to fetch data from.
     * @param prompt     The prompt for the language model.
     * @param query      Additional query parameters.
     * @param maxTokens  Maximum number of tokens for the response.
     * @param auth       Authorization token in Bearer format.
     * @return           JSON response containing language model results.
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
        if (!Validate.isBearer(auth)) {
            log.info("api.llm.oai#protected");
            if (!Validate.isUnGuarded())
                return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
        }
        if (Validate.isNonAlphanumeric(repo)) {
            return new OopsResponse("api.llm.oai#repository-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (Validate.isNonAlphanumeric(model)) {
            return new OopsResponse("api.llm.oai#model-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (Validate.isNonAlphanumeric(prompt)) {
            return new OopsResponse("api.llm.oai#prompt-invalid", Response.Status.BAD_REQUEST).asJSON();
        }
        if (Validate.isNonAlphanumeric(query)) {
            return new OopsResponse("api.llm.oai#query-invalid", Response.Status.BAD_REQUEST).asJSON();
        }

        String TOKEN = System.getenv("OPENAI_API_KEY");
        if (Validate.isMissing(TOKEN)) {
            return new OopsResponse("api.llm.oai#not-configured", Response.Status.SERVICE_UNAVAILABLE).asJSON();
        }
        ChatGPT gpt = new ChatGPT(System.getenv("OPENAI_API_KEY"), 100);
        ChatThread chat = new ChatThread();
        chat.add("user", query);
        I_Thread<String> generated = gpt.generate(chat);

        System.out.println("api.llm.openai.generated: "+generated.messages());
        // to BODY into `JSON`
//        Map<String, Object> map = objectMapper.readValue(body, Map.class);

        return new SimpleResponse(generated).asJSON();
    }
}
