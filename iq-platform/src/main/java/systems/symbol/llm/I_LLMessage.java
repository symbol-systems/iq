package systems.symbol.llm;

public interface I_LLMessage<T> {

enum MessageType {
TEXT, IMAGE, JSON, AUDIO, VIDEO, FUNCTION, URL, DATA ,OTHER
}

MessageType getType();
String getRole();
String getName();
T getContent();
}
