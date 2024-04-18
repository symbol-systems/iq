package systems.symbol.skill;

import org.eclipse.rdf4j.model.Literal;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.ChatThread;
import systems.symbol.llm.I_Thread;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.COMMONS;
import systems.symbol.platform.Platform;
import systems.symbol.rdf4j.store.IQConnection;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.render.HBSRenderer;

import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.IOException;

/**
 * Builder class for constructing a QuerySkill instance.
 */
public class LLMSkillBuilder<T> extends AbstractSkillBuilder {
private RepositoryConnection connection;
private String promptPath;
private String model;
private int maxTokens = 100;
private double temperature = 1.0;

public LLMSkillBuilder(Platform platform, RepositoryConnection connection, String promptPath, String model, int maxTokens) {
super(platform);
this.connection = connection;
this.promptPath = (promptPath == null) ? "prompt/default" : promptPath;
this.model = model;
this.maxTokens = maxTokens;
}


public I_SkillBuilder withTemperature(double temperature) {
this.temperature = temperature;
return this;
}

public I_SkillBuilder withConnection(RepositoryConnection connection) {
this.connection = connection;
return this;
}

public I_SkillBuilder withPromptPath(String promptPath) {
this.promptPath = promptPath;
return this;
}

@Override
public QuerySkill build() throws SkillException {
// Lookup LLM task prompt in the platform repository
IQConnection iq = new IQConnection(platform.getSelf(), connection);
ScriptCatalog library = new ScriptCatalog(iq);
IRI promptIRI = iq.toIRI(this.promptPath + ".txt");
IRI mimeIRI = iq.toIRI(COMMONS.MIME_PLAIN);
Literal taskPrompt = library.getContent(promptIRI, mimeIRI);

// check prompt exists and has placeholders
if (taskPrompt == null) {
throw new SkillException("api.llm.openai#prompt-missing", 404);
}

return new QuerySkill<T>(this, taskPrompt);
}

/**
 * Inner class representing the constructed QuerySkill.
 */
public class QuerySkill<T> implements I_Skill<T> {
private final String taskPrompt;

public QuerySkill(LLMSkillBuilder builder, String taskPrompt) {
this.taskPrompt = taskPrompt;
}

public QuerySkill(LLMSkillBuilder<T> builder, Literal taskPrompt) {
this.taskPrompt = taskPrompt.stringValue();
}

@Override
public T perform() throws SkillException {
try {
return performLLM();
} catch (Exception e) {
  throw new SkillException(e.getLocalizedMessage(), Response.Status.INTERNAL_SERVER_ERROR);
} catch (APIException e) {
throw new RuntimeException(e);
}
}

public T performLLM() throws SkillException, IOException, APIException {
String finalPrompt = HBSRenderer.template(taskPrompt, bindings);

// Set up the LLM
String apiKey = System.getenv("OPENAI_API_KEY");
int tokensMax = Math.min(4000, Math.max(100, maxTokens));
if ("default".equals(model)) {
//model = OpenAiModelName.GPT_3_5_TURBO;
}
ChatGPT gpt = new ChatGPT(apiKey, tokensMax);

//ChatLanguageModel llm = OpenAiChatModel.builder()
//.apiKey(apiKey)
//.modelName(model)
//.temperature(1.0)
//.maxTokens(tokensMax)
//.build();
//
ChatThread thread = new ChatThread();
thread.user(finalPrompt);
I_Thread answer = gpt.generate(thread);
//List<ChatMessage> messages = new ArrayList<>();
//messages.add(new UserMessage(finalPrompt));
// Execute final prompt against the LLM
//dev.langchain4j.model.output.Response<AiMessage> answer = llm.generate(messages);
//incur(answer.tokenUsage().totalTokenCount());
//messages.add(answerN.content());
return (T)answer.messages();
}
}
}
