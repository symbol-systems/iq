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

public class GenericGPT implements I_LLM<String> {

protected final Logger log = LoggerFactory.getLogger(getClass());
ObjectMapper om = new ObjectMapper();
String token;
I_LLMConfig config;
private final List<GPTResponse> history;
private int retryCount = 2;
private int backOffTime = 2000;
private List<Tool> tools = new ArrayList<>();
private List<Usage> usage = new ArrayList<>();

public GenericGPT(String token, I_LLMConfig config) {
this.token = token;
this.config = config;
this.history = new ArrayList<>();
init();
}

public GenericGPT(String token, I_LLMConfig config, int retryCount) {
this.token = token;
this.history = new ArrayList<>();
this.config = config;
this.retryCount = retryCount;
init();
}

// public GenericGPT(String token, int contextLength) {
// this.token = token;
// this.config = LLMFactory.GPT3_5_Turbo(contextLength);
// this.history = new ArrayList<>();
// init();
// }

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
return processAttempt(chats, retryCount);
}

protected I_Assist<String> processAttempt(I_Assist<String> chats, int attempt) throws APIException, IOException {
if (attempt <= 0) {
log.debug("llm.gpt.throttled: {} -> {}", config.getName(), attempt);
return chats;
}
log.info("llm.gpt.url: {} @ {} -> {}", config.getName(), config.getURL(), chats.latest());
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
} else
return processAttempt(chats, attempt - 1);
} catch (Exception e) {
log.info("oops.llm.gpt.fatal # {}: {}", attempt, e.getMessage());
return processAttempt(chats, attempt - 1);
}
}

private I_Assist<String> processError(I_Assist<String> chats, int attempt, GPTResponse completion,
Response response) throws IOException, APIException {
if (completion.error.failed_generation == null) {
log.info("oops.llm.gpt.error: {} -> {} => {}", completion.error.code, completion.error.message,
completion.error.type);
return processAttempt(chats, attempt);
}
if (response.code() == 429) {
try {
String retry = response.header("retry-after");
int backoff = retry == null ? backOffTime : 1000 * Integer.parseInt(retry);
log.info("llm.gpt.backoff: {}ms -> {}", backoff, completion.error);
Thread.sleep(backoff);
return processAttempt(chats, attempt);
} catch (InterruptedException e) {
log.info("llm.gpt.interrupted: {}", e.getMessage());
}
}
log.info("oops.llm.gpt.generate: {} / {} => {}", response.code(), completion.error.code,
completion.error.failed_generation);
return processAttempt(chats, attempt);
}

private I_Assist<String> processReply(I_Assist<String> chats, GPTResponse completion)
throws JsonMappingException, JsonProcessingException, StateException {
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
throws JsonProcessingException, StateException {
GPTResponse.Message message = choice.message;
I_LLMessage.RoleType role = I_LLMessage.RoleType.assistant;
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
chat.add(new TextMessage(role, content));
}

private void processThinking(I_Assist<String> chat, GPTResponse.Message message, int thinks)
throws JsonMappingException, JsonProcessingException, StateException {
I_LLMessage.RoleType role = I_LLMessage.RoleType.assistant;
int start = message.content.indexOf("<think>");
String thoughts = message.content.substring(start + 7, thinks);
String answer = message.content.substring(thinks + 8).trim();
chat.add(new TextMessage(I_LLMessage.MessageType.thoughts, role, thoughts));
log.info("llm.gpt.thoughts: {} --> {}", thoughts, answer);
if (answer.startsWith("{"))
processJSON(chat, answer);
else
chat.add(new TextMessage(I_LLMessage.MessageType.text, role, answer));
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
