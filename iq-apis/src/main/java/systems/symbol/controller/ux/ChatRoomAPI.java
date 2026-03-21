package systems.symbol.controller.ux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import systems.symbol.chat.ChatRoom;
import systems.symbol.llm.Conversation;

@ServerEndpoint("/chat/{realm}/{room}")
@ApplicationScoped
@Authenticated
public class ChatRoomAPI {
protected final Logger log = LoggerFactory.getLogger(getClass());
private static final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

@OnOpen
public void onOpen(Session session, @PathParam("realm") String realm, @PathParam("room") String room) {
String username = session.getUserPrincipal() != null ? session.getUserPrincipal().getName() : "Guest";
rooms.computeIfAbsent(room, k -> new ChatRoom(new Conversation())).addParticipant(session, username);
}

@OnMessage
public void onMessage(String message, @PathParam("realm") String realm, @PathParam("room") String room,
Session session) {
ChatRoom chatRoom = rooms.get(room);
if (chatRoom != null) {
String username = rooms.get(room).getParticipant(session);
chatRoom.broadcast(username, message);
}
}

@OnClose
public void onClose(Session session, @PathParam("realm") String realm, @PathParam("room") String room) {
ChatRoom chatRoom = rooms.get(room);
if (chatRoom != null) {
chatRoom.removeParticipant(session);
if (chatRoom.isEmpty()) {
rooms.remove(room);
}
}
}

@OnError
public void onError(Session session, Throwable throwable) {
log.error("WebSocket error in chat room", throwable);
}
}
