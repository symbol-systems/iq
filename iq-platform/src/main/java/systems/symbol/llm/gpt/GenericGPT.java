package systems.symbol.llm.gpt;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.llm.*;
import systems.symbol.string.Validate;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericGPT implements I_LLM<String> {
    private static final String MD_BLOCKS_REGEX = "```(?:([a-zA-Z0-9_]+))?\\n([\\s\\S]*?)\\n```";
    private static final String JSON_BLOCKS_REGEX = "\\{[^{}]*\\}";

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
        log.debug("llm.gpt.url: {} -> {}", config.getName(), config.getURL());
        RestAPI api = new RestAPI(config.getURL());
        api.header("Authorization", "Bearer " + token);

        Map<String, Object> json = toPayload(null, chats.messages());

        String body;
        int attempts = 0;
        while (attempts < retryCount) {
            attempts++;
            try (okhttp3.Response response = api.post(json)) {
                log.debug("llm.gpt.response: {} -> {}", response.code(), response.message());

                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    body = responseBody.string();
                    GPTResponse completion = om.readValue(body, GPTResponse.class);
                    log.debug("llm.gpt.status: {} x {} tokens", response.code(), completion.usage.total_tokens);
                    if (completion.choices != null && !completion.choices.isEmpty()) {
                        for (int c = 0; c < completion.choices.size(); c++) {
                            GPTResponse.Choice choice = completion.choices.get(c);
                            processMessage(chats, choice.message);
                        }
                        history.add(completion);
                        log.debug("llm.gpt.complete: {}", chats.latest());
                        return chats;
                    }
                }
            } catch (Exception e) {
                log.info("llm.gpt.error # {}: {}", attempts, e.getMessage());
            }
        }
        return chats;
    }

    private void processMessage(I_Assist<String> chat, GPTResponse.Message message) throws JsonProcessingException {
        I_LLMessage.RoleType role = I_LLMessage.RoleType.assistant;
        Pattern pattern = Pattern.compile(JSON_BLOCKS_REGEX + "|" + MD_BLOCKS_REGEX);
        Matcher blocks = pattern.matcher(message.content);
        boolean matchFound = false;

        while (blocks.find()) {
            String block = blocks.group();

            if (block.matches(JSON_BLOCKS_REGEX)) {
                try {
                    SimpleBindings decision = om.readValue(block, SimpleBindings.class);
                    String content = String.valueOf(decision.get("content"));
                    String intent = String.valueOf(decision.get("intent"));
                    if (intent != null) {
                        if (intent.indexOf(":") > 0)
                            chat.add(new IntentMessage(intent, role, content, decision));
                        else
                            chat.add(new TextMessage(role, content));
                    } else {
                        chat.add(new TextMessage(role, block));
                    }
                    log.info("llm.gpt.intent: {} => {}", intent, chat.latest());
                } catch (JsonProcessingException e) {
                    log.error("llm.gpt.error: {}", block, e);
                    chat.add(new TextMessage(role, block));
                }
            } else {
                log.info("llm.gpt.message: {} => {}", message.content, block);
                chat.add(new TextMessage(role, block));
            }
            matchFound = true;
        }

        if (!matchFound) {
            log.debug("llm.gpt.message: {}", message.content);
            chat.add(new TextMessage(role, message.content));
        }
    }

    private Map<String, Object> toPayload(String response_format_type, List<I_LLMessage<String>> msgs) {
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
        if (response_format_type != null) {
            Map<String, Object> response_format = RestAPI.newParams();
            response_format.put("type", response_format_type);
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
