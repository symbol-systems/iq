package systems.symbol.controllers.llm;

import systems.symbol.platform.APIPlatform;
import systems.symbol.responses.OopsResponse;
import systems.symbol.responses.SimpleResponse;
import systems.symbol.skill.LLMSkillBuilder;
import systems.symbol.skill.SkillException;
import systems.symbol.string.Validate;
import dev.langchain4j.data.message.ChatMessage;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST-ful API for Large Language Model (LLM) endpoints.
 */
@Path("llm")
public class LLM {

@Inject
APIPlatform platform;

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
@Path("openai/{model}/{repo}/{prompt: .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response answer(@PathParam("model") String model,
   @PathParam("repo") String repo,
   @PathParam("prompt") String prompt,
   @QueryParam("query") String query,
   @QueryParam("maxTokens") int maxTokens,
   @HeaderParam("Authorization") String auth
) {

// Check for valid Bearer token in the Authorization header
if (!Validate.isBearer(auth)) {
//return new OopsResponse("api.llm.openai#authentication-required", Response.Status.UNAUTHORIZED).asJSON();
}

// Get the repository instance from the platform
Repository repository = platform.getRepository(repo);
if (repository == null) {
return new OopsResponse("api.llm.openai#repository-missing", Response.Status.NOT_FOUND).asJSON();
}

try (RepositoryConnection connection = repository.getConnection()) {
// Set up bindings for the language model query
Map<String, Object> bindings = new HashMap<>();
bindings.put("query", query);

// Build and execute the Language Model skill
LLMSkillBuilder<List<ChatMessage>> querySkillBuilder = new LLMSkillBuilder<>(platform, connection, prompt, model, maxTokens);
querySkillBuilder.withBindings(bindings);
querySkillBuilder.withTemperature(1.0);
LLMSkillBuilder<List<ChatMessage>>.QuerySkill<List<ChatMessage>> skill = querySkillBuilder.build();
List<ChatMessage> perform = skill.perform();

// Return the response as JSON
return new SimpleResponse(perform).asJSON();
} catch (SkillException e) {
return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
}
}
}
