package systems.symbol.llm;

public class TextMessage extends AbstractMessage<String> {
public TextMessage(String name, String role, String content) {
super(name, role, content);
this.type = MessageType.TEXT;
}
}
