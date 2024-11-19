package systems.symbol.llm.gpt;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.*;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericGPT implements I_LLM<String> {
// private static final String MD_BLOCKS_REGEX =
// "```(?:([a-zA-Z0-9_]+))?\\n([\\s\\S]*?)\\n```";
// private static final String JSON_BLOCKS_REGEX = "\\{[^{}]*\\}";

protected final Logger log = LoggerFactory.getLogger(getClass());
ObjectMapper om = new ObjectMapper();
String token;
I_LLMConfig config;
private final List<GPTResponse> history;
private int retryCount = 1;

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

public GenericGPT(String token, int contextLength) {
this.token = token;
this.config = CommonLLM.GPT3_5_Turbo(contextLength);
this.history = new ArrayList<>();
init();
}

private void init() {
om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
}

public I_LLMConfig getConfig() {
return config;
}

@Override
public I_Assist<String> complete(I_Assist<String> chats) throws APIException, IOException {
return complete(chats, 0);
}

protected I_Assist<String> complete(I_Assist<String> chats, int attempt) throws APIException, IOException {
if (attempt > retryCount)
return chats;
log.debug("llm.gpt.url: {} -> {}", config.getName(), config.getURL());
RestAPI api = new RestAPI(config.getURL());
api.header("Authorization", "Bearer " + token);

Map<String, Object> json = toPayload(chats.messages());

String body;
try (okhttp3.Response response = api.post(json)) {
log.debug("llm.gpt.response: {} -> {}", response.code(), response.message());

ResponseBody responseBody = response.body();
if (responseBody != null) {
body = responseBody.string();
GPTResponse completion = om.readValue(body, GPTResponse.class);
int tokens = completion.usage == null ? -1 : completion.usage.total_tokens;
log.info("llm.gpt.reply: [ #{} ] {} x {} tokens", attempt, response.code(), tokens);
if (completion.choices != null && !completion.choices.isEmpty()) {
for (int c = 0; c < completion.choices.size(); c++) {
GPTResponse.Choice choice = completion.choices.get(c);
processMessage(chats, choice.message);
}
history.add(completion);
log.info("llm.gpt.ok: {} => {} x {} tokens", completion.choices.size(), chats.latest(), tokens);
return chats;
} else if (completion.error != null) {
// completion.error.failed_generation
if (completion.error.failed_generation != null) {
log.info("llm.gpt.oops: {} / {} => {}", response.code(), completion.error.code,
completion.error.failed_generation);
complete(chats, attempt++);
// chats.add(new TextMessage(I_LLMessage.RoleType.assistant,
// completion.error.failed_generation));
} else {
log.info("llm.gpt.OOPS: {} -> {} => {}", completion.error.code, completion.error.message,
completion.error.type);
complete(chats, attempt++);
// chats.add(new TextMessage(I_LLMessage.RoleType.assistant, "llm.oops." +
// completion.error));
}
}
}
} catch (Exception e) {
log.info("llm.gpt.fatal # {}: {}", attempt, e.getMessage());
complete(chats, attempt++);
}
return chats;
}

private void processMessage(I_Assist<String> chat, GPTResponse.Message message)
throws JsonProcessingException, StateException {
I_LLMessage.RoleType role = I_LLMessage.RoleType.assistant;
if (!message.content.startsWith("{")) {
chat.add(new TextMessage(role, message.content));
return;
}
try {
SimpleBindings json = om.readValue(message.content, SimpleBindings.class);
IntentMessage intent = new IntentMessage(json);
chat.add(intent);
log.info("llm.gpt.intent: {}", intent.intent());
} catch (JsonProcessingException e) {
log.error("llm.gpt.error: {}", message.content, e);
chat.add(new TextMessage(role, message.content));
}
}

private Map<String, Object> toPayload(List<I_LLMessage<String>> msgs) {
Map<String, Object> json = RestAPI.newParams();
json.put("model", config.getName());
json.put("temperature", config.getTemperature());
json.put("frequency_penalty", config.getFrequencyPenalty());
json.put("seed", config.getSeed());
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
}
