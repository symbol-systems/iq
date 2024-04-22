package systems.symbol.llm;

public class TextMessage extends AbstractMessage<String> {
    public TextMessage(String role, String content) {
        super(role, content);
        this.type = MessageType.TEXT;
    }
}
