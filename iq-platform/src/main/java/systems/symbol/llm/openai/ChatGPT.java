package systems.symbol.llm.openai;

import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.llm.*;
import systems.symbol.secrets.EnvsAsSecrets;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static systems.symbol.llm.openai.CommonLLM.OPENAI_COMPLETIONS;

public class ChatGPT implements I_LLM<String> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    ObjectMapper objectMapper = new ObjectMapper();
    String token;
    I_LLMConfig config;

    public ChatGPT(String token, I_LLMConfig config) {
        this.token = token;
        this.config = config;
    }

//    public ChatGPT(I_Secrets secrets, I_LLMConfig config) {
//        this.token = secrets.getSecret(OPENAI_COMPLETIONS);
//        this.config = config;
//    }

    public ChatGPT(String token, int tokens) {
        this.token = token;
        this.config = newGPT3_5_Turbo(tokens);
    }

    I_LLMConfig newGPT3_5_Turbo(int tokens) {
        return new DefaultLLConfig(OPENAI_COMPLETIONS, "gpt-3.5-turbo-0125", tokens);
    }

    @Override
    public I_LLMConfig getConfig() {
        return config;
    }

    @Override
    public I_Thread<String> generate(I_Thread<String> chats) throws APIException, IOException {

        RestAPI api = new RestAPI(config.getURL(), token);

        Map<String, Object> json = toPayload(null, chats.messages()); // "json_object"
        log.info("api.openai.post: {}", json);

        String body;
        try (okhttp3.Response response = api.post(json)) {
            log.info("api.openai.response: {} -> {}", response.code(), response.message());

            // to BODY into `JSON`
            ResponseBody responseBody = response.body();
            log.info("api.openai.body: {}", responseBody);
            if (responseBody != null) {
                body = responseBody.string();
                ChatGPTResponse completion = objectMapper.readValue(body, ChatGPTResponse.class);
                if (completion!=null) {
                    ChatGPTResponse.Message message = completion.choices.get(0).message;
                    log.info("api.openai.completion: {} -> {}", completion.choices, message);
                    chats.add(new TextMessage("assistant", message.role, message.content));
                }
            } else throw new IOException();
        }
        return chats;
    }

    @Override
    public boolean isOnline() {
        return !Validate.isMissing(token);
    }

    private Map<String, Object> toPayload(String response_format_type, List<I_LLMessage<String>> msgs) {
        Map<String, Object> json = RestAPI.newParams();
        json.put("model", config.getName());
        json.put("temperature", config.getTemperature());
        json.put("frequency_penalty", config.getFrequencyPenalty());
//        json.put("top_p", config.getTopP());
        json.put("seed", config.getSeed());
        List<Map<String, Object>> messages = new ArrayList<>();
        for (I_LLMessage<String> msg : msgs) {
            if (msg.getType() == I_LLMessage.MessageType.TEXT) {
                messages.add(toMap(msg));
            }
        }
        json.put("messages", messages);
        if (response_format_type!=null) {
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
}
