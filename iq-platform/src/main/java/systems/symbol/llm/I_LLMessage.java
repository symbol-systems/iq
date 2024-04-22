package systems.symbol.llm;

public interface I_LLMessage<T> {

    enum MessageType {
        TEXT, IMAGE, JSON, AUDIO, VIDEO, FUNCTION, URL, DATA ,OTHER
    }

    enum RoleType {
        system, assistant, user
    }

    MessageType getType();
    RoleType getRole();
    T getContent();
}
