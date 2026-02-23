package systems.symbol.llm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = LLMessageDeserializer.class)
public interface I_LLMessage<T> {

    enum MessageType {
        text, image_url, intent, JSON, audio, video, fn, url, data, error, thoughts
    }

    enum RoleType {
        system, assistant, user
    }

    MessageType getType();

    RoleType getRole();

    T getContent();
}
