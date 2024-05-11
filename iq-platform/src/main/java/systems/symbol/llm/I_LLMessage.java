package systems.symbol.llm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = LLMessageDeserializer.class)
public interface I_LLMessage<T> {

    enum MessageType {
        text, image, JSON, audio, video, fn, url, data, error
    }

    enum RoleType {
        system, assistant, user
    }

    MessageType getType();
    RoleType getRole();
    T getContent();
}
