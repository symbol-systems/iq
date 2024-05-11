package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.llm.I_Chat;
import systems.symbol.llm.I_LLMessage;

import java.util.List;

public class ChatResponse implements I_Response {
public List<I_LLMessage<String>> messages;
public ChatResponse(I_Chat<String> chat) {
this.messages = chat.messages();
}

public Response asJSON() {
Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE).entity(this);
return addCORS(builder).build();
}
}
