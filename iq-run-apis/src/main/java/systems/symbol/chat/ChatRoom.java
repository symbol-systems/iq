package systems.symbol.chat;

import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.llm.TextMessage;

import jakarta.websocket.Session;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {
private static final Jsonb jsonb = JsonbBuilder.create();
private final Map<Session, String> participants = new ConcurrentHashMap<>();
private final Conversation chat;

public ChatRoom(Conversation chat) {
this.chat = chat;
}

public void addParticipant(Session session, String username) {
participants.put(session, username);
sendChatHistory(session);
broadcast(username, "joined the chat");
}

public void removeParticipant(Session session) {
String username = participants.remove(session);
if (username != null) {
broadcast(username, "left the chat");
}
}

public void broadcast(String from, String content) {
I_LLMessage<String> message = new TextMessage(I_LLMessage.RoleType.user, content);
chat.add(message);
String jsonMessage = jsonb.toJson(Map.of("message", message));

participants.keySet().forEach(session -> {
try {
if (session.isOpen()) {
session.getBasicRemote().sendText(jsonMessage);
}
} catch (IOException e) {
e.printStackTrace();
}
});
}

private void sendChatHistory(Session session) {
try {
if (session.isOpen()) {
String historyJson = jsonb.toJson(Map.of("messages", chat.messages()));
session.getBasicRemote().sendText(historyJson);
}
} catch (IOException e) {
e.printStackTrace();
}
}

public boolean isEmpty() {
return participants.isEmpty();
}

public String getParticipant(Session session) {
return participants.get(session);
}
}
