package systems.symbol.chat;

import java.util.Set;

public interface I_ChatMessage<T> {
    String from();
    String getRole();
    T getContent();
    Set<String> getTags();
}
