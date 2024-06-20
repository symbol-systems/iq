package systems.symbol.llm.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.agent.tools.RestAPI;
import systems.symbol.llm.*;
import systems.symbol.string.Validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericGPT implements I_LLM<String> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    ObjectMapper objectMapper = new ObjectMapper();
    String token;
    I_LLMConfig config;

    public GenericGPT(String token, I_LLMConfig config) {
        this.token = token;
        this.config = config;
    }

    public GenericGPT(String token, int tokens) {
        this.token = token;
        this.config = CommonLLM.newGPT3_5_Turbo(tokens);
    }


    @Override
    public I_LLMConfig getConfig() {
        return config;
    }

    @Override
    public void complete(I_Chat<String> chats) throws APIException, IOException {

        RestAPI api = new RestAPI(config.getURL());
        api.header("Authorization", "Bearer "+token);

        Map<String, Object> json = toPayload(null, chats.messages()); // "json_object"
//        log.info("api.gpt.post: {}", json);

        String body;
        try (okhttp3.Response response = api.post(json)) {
            log.info("api.gpt.response: {} -> {}", response.code(), response.message());

            // to BODY into `JSON`
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                body = responseBody.string();
                log.info("api.gpt.body: {}", body);
                GPTResponse completion = objectMapper.readValue(body, GPTResponse.class);
                if (completion!=null) {
                    GPTResponse.Message message = completion.choices.get(0).message;
                    log.info("api.gpt.completion: {} -> {}", completion.choices, message);
                    chats.add(new TextMessage(message.role, message.content));
                }
            } else throw new IOException();
        }
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
            if (msg.getType() == I_LLMessage.MessageType.text) {
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
