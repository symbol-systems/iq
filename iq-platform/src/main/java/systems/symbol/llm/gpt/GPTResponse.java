package systems.symbol.llm.gpt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GPTResponse {

    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices;
    public Usage usage;
    public String system_fingerprint;
    public String refusal;
    public Error error;

    public GPTResponse() {
    }

    public static class Choice {
        public int index;
        public Message message;
        public Boolean logprobs;
        public String finish_reason;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        public String role;
        public String refusal;
        public String content;
        public List<ToolCall> tool_calls;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolCall {
        public String id;
        public String type;
        public Function function;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Function {
        public String name;
        public String arguments;
    }

    public static class Usage {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
        public float prompt_time;
        public float completion_time;
        public float total_time;
        public float queue_time;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        public String message, type, param, code, failed_generation;
    }
}
