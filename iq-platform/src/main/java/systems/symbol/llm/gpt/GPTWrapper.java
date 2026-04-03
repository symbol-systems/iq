package systems.symbol.llm.gpt;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.tools.APIException;
import systems.symbol.tools.RestAPI;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.*;
import systems.symbol.llm.gpt.GPTResponse.ToolCall;
import systems.symbol.llm.gpt.GPTResponse.Usage;
import systems.symbol.llm.tools.Tool;
import systems.symbol.string.PrettyStrings;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GPTWrapper implements I_LLM<String> {

protected final Logger log = LoggerFactory.getLogger(getClass());
ObjectMapper om = new ObjectMapper();
String token;
I_LLMConfig config;
private final List<GPTResponse> history;
private int retryCount = 3;
private int backOffInitialMs = 200;
private int maxBackOffMs = 30000;
private List<Tool> tools = new ArrayList<>();
private List<Usage> usage = new ArrayList<>();private I_OutputSchema outputSchema;

public GPTWrapper(String token, I_LLMConfig config) {
this.token = token;
this.config = config;
this.history = new ArrayList<>();
init();
}

public GPTWrapper(String token, I_LLMConfig config, int retryCount) {
this.token = token;
this.history = new ArrayList<>();
this.config = config;
this.retryCount = retryCount;
init();
}

private void init() {
om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
}

public I_LLMConfig getConfig() {
return config;
}

public List<Usage> getUsage() {
return usage;
}

public void tool(Tool tool) {
tools.add(tool);
}

@Override
public I_Assist<String> complete(I_Assist<String> chats) throws APIException, IOException {
I_TokenCounter counter = new WordTokenCounter();
int maxTokens = config.getMaxTokens();
I_Assist<String> scoped = maxTokens > 0 ? Conversation.withTokenBudget(chats, counter, maxTokens) : chats;
log.info("llm.gpt.tokenBudget: maxTokens={} used={} originalMessages={} trimmedMessages={}",
maxTokens,
scoped.tokensUsed(counter),
chats == null ? 0 : chats.messages().size(),
scoped == null ? 0 : scoped.messages().size());
return processAttempt(scoped, retryCount);
}

public void setOutputSchema(I_OutputSchema schema) {
this.outputSchema = schema;
}

protected I_Assist<String> processAttempt(I_Assist<String> chats, int attempt) throws APIException, IOException {
if (attempt <= 0) {
log.debug("llm.gpt.throttled: {} -> {}", config.getName(), attempt);
return chats;
}
log.info("llm.gpt.api: {} @ {} -> {}", config.getName(), config.getURL(), chats.latest());
RestAPI api = new RestAPI(config.getURL());
api.header("Authorization", "Bearer " + token);
Map<String, Object> json = toPayload(chats.messages());
try (okhttp3.Response response = api.post(json)) {
log.debug("llm.gpt.response: {} -> {}", response.code(), response.message());

ResponseBody responseBody = response.body();
if (responseBody != null) {
String body = responseBody.string();
GPTResponse completion = om.readValue(body, GPTResponse.class);
if (response.code() == 200 && completion.choices != null && !completion.choices.isEmpty()) {
return processReply(chats, completion);
} else {
return processError(chats, attempt - 1, completion, response);
}
} else {
sleepBackoff(attempt);
return processAttempt(chats, attempt - 1);
}
} catch (StateException e) {
// Wrap state exceptions as API errors and retry
log.warn("oops.llm.gpt.state # {}: {}", attempt, e.getMessage());
sleepBackoff(attempt);
return processAttempt(chats, attempt - 1);
} catch (IOException e) {
log.info("oops.llm.gpt.fatal # {}: {}", attempt, e.getMessage());
sleepBackoff(attempt);
return processAttempt(chats, attempt - 1);
} catch (APIException e) {
log.warn("oops.llm.gpt.schema # {}: {}", attempt, e.getMessage());
if (attempt <= 1) {
throw e;
}
sleepBackoff(attempt);
return processAttempt(chats, attempt - 1);
}
}

private I_Assist<String> processError(I_Assist<String> chats, int attempt, GPTResponse completion,
Response response) throws IOException, APIException {
if (completion.error == null || completion.error.failed_generation == null) {
String code = completion.error != null ? completion.error.code : "unknown";
String msg = completion.error != null ? completion.error.message : "unknown";
String type = completion.error != null ? completion.error.type : "unknown";
log.warn("oops.llm.gpt.error: {} -> {} => {}", code, msg, type);
sleepBackoff(attempt);
return processAttempt(chats, attempt);
}
if (response.code() == 429 || response.code() == 503 || response.code() == 500) {
try {
String retry = response.header("retry-after");
long backoff = retry == null ? sleepBackoff(attempt) : Math.min(maxBackOffMs, 1000L * Long.parseLong(retry));
if (retry != null) {
log.info("llm.gpt.backoff: {}ms (Retry-After) -> {}", backoff, completion.error);
Thread.sleep(backoff);
return processAttempt(chats, attempt);
}
return processAttempt(chats, attempt);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
log.info("llm.gpt.interrupted: {}", e.getMessage());
}
}
log.info("oops.llm.gpt.generate: {} / {} => {}", response.code(), completion.error.code,
completion.error.failed_generation);
sleepBackoff(attempt);
return processAttempt(chats, attempt);
}

/**
 * Exponential backoff with jitter.
 *
 * @param attempt The remaining retry attempts for the current request
 * @return the sleep duration (ms)
 */
private long sleepBackoff(int attempt) {
int exponent = Math.max(0, retryCount - attempt);
long base = backOffInitialMs * (1L << Math.min(exponent, 30));
long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, base / 2));
long delay = Math.min(maxBackOffMs, base + jitter);
try {
Thread.sleep(delay);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
log.info("llm.gpt.backoff.interrupted: {}", e.getMessage());
}
log.debug("llm.gpt.backoff.sleep: {}ms (attempt {} of {})", delay, attempt, retryCount);
return delay;
}

private I_Assist<String> processReply(I_Assist<String> chats, GPTResponse completion)
throws JsonMappingException, JsonProcessingException, StateException, APIException {
for (int c = 0; c < completion.choices.size(); c++) {
GPTResponse.Choice choice = completion.choices.get(c);
if (choice.finish_reason.equals("tool_calls")) {
for (int t = 0; t < choice.message.tool_calls.size(); t++) {
processTool(choice.message.tool_calls.get(t), chats);
}
} else {
processMessage(chats, choice);
}
usage.add(completion.usage);
}
history.add(completion);
return chats;
}

private void processTool(ToolCall tool, I_Assist<String> chats)
throws JsonMappingException, JsonProcessingException {
SimpleBindings meta = om.readValue(tool.function.arguments, SimpleBindings.class);
IntentMessage intent = new IntentMessage(tool.function.name, meta);
log.info("llm.gpt.tool: {} -> {} == {} -> {}", tool.id, tool.type, tool.function.name,
tool.function.arguments);
chats.add(intent);
}

private void processMessage(I_Assist<String> chat, GPTResponse.Choice choice)
throws JsonMappingException, JsonProcessingException, StateException, APIException {
GPTResponse.Message message = choice.message;
I_LLMessage.RoleType role = I_LLMessage.RoleType.assistant;
if (message.content == null) {
log.debug("llm.gpt.message.null-content: choice={}", choice.finish_reason);
return;
}
String content = message.content.trim();
log.info("llm.gpt.message: {}", content);

if (content.startsWith("{")) {
processJSON(chat, message.content);
return;
}
int thinks = content.indexOf("</think>");
if (thinks >= 0) {
processThinking(chat, message, thinks);
return;
}
validateResponseSchema(content);
chat.add(new TextMessage(role, content));
}

void validateResponseSchema(String content) throws APIException {
String format = config.getResponseFormat();
if (format == null || !"json".equalsIgnoreCase(format)) {
return;
}
content = content.trim();
if (content.isEmpty()) {
throw new APIException("llm.gpt.response.empty", config.getURL(), null);
}
if (outputSchema != null) {
java.util.List<String> violations = outputSchema.validate(content);
if (!violations.isEmpty()) {
log.warn("llm.gpt.schema.validation.errors: {}", violations);
throw new APIException("llm.gpt.response.schema.invalid: " + violations, config.getURL(), null);
}
}
try {
om.readTree(content);
} catch (Exception e) {
log.warn("llm.gpt.schema.invalid: {}", e.getMessage());
throw new APIException("llm.gpt.response.schema.invalid", config.getURL(), null);
}
}

private void processThinking(I_Assist<String> chat, GPTResponse.Message message, int thinks)
throws JsonMappingException, JsonProcessingException, StateException, APIException {
I_LLMessage.RoleType role = I_LLMessage.RoleType.assistant;
int start = message.content.indexOf("<think>");
String thoughts = message.content.substring(start + 7, thinks);
String answer = message.content.substring(thinks + 8).trim();
chat.add(new TextMessage(I_LLMessage.MessageType.thoughts, role, thoughts));
log.info("llm.gpt.thoughts: {} --> {}", thoughts, answer);
if (answer.startsWith("{"))
processJSON(chat, answer);
else
chat.add(new TextMessage(role, answer));
}

private void processJSON(I_Assist<String> chat, String message)
throws JsonMappingException, JsonProcessingException, StateException {
SimpleBindings json = om.readValue(message, SimpleBindings.class);
IntentMessage intent = new IntentMessage(json);
log.info("llm.gpt.json: {} --> {} ---> {}", intent.intent(), intent.getContent(), PrettyStrings.pretty(json));
chat.add(intent);
}

private Map<String, Object> toPayload(List<I_LLMessage<String>> msgs) {
Map<String, Object> json = RestAPI.newParams();
json.put("model", config.getName());
json.put("temperature", config.getTemperature());
json.put("frequency_penalty", config.getFrequencyPenalty());
json.put("seed", config.getSeed());
if (tools != null && tools.size() > 0) {
json.put("tools", tools);
json.put("tool_choice", "auto");
log.info("llm.gpt.tools: {}", tools);
}
List<Map<String, Object>> messages = new ArrayList<>();
for (I_LLMessage<String> msg : msgs) {
if (msg.getType() == I_LLMessage.MessageType.text) {
messages.add(toMap(msg));
}
}
json.put("messages", messages);
if (config.getResponseFormat() != null) {
Map<String, Object> response_format = RestAPI.newParams();
response_format.put("type", config.getResponseFormat());
json.put("response_format", response_format);
}
return json;
}

// public void addTool(I_ToolSpec tool) {
// this.tools.add(tool);
// }

private Map<String, Object> toMap(I_LLMessage<String> chat) {
Map<String, Object> message = RestAPI.newParams();
message.put("role", chat.getRole());
message.put("content", chat.getContent());
return message;
}

@Override
public java.util.stream.Stream<I_LLMessage<String>> stream(I_Assist<String> chat) throws APIException, IOException {
// Best-effort streaming; currently materialized from complete()
I_Assist<String> result = complete(chat);
if (result == null || result.messages() == null) {
return java.util.stream.Stream.empty();
}
return result.messages().stream();
}

public List<GPTResponse> getHistory() {
return history;
}

public String toString() {
return getConfig().getName();
}

@Override
public Collection<Tool> tools() {
return tools;
}
}
