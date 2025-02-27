package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import systems.symbol.agent.Facades;
import systems.symbol.agent.I_Agent;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLMessage;
import systems.symbol.rdf4j.Facts;

import java.util.Collection;
import java.util.List;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.eclipse.rdf4j.model.Resource;

public class ChatResponse implements I_Response {
    public Bindings meta = null;
    public List<I_LLMessage<String>> messages;

    public ChatResponse(I_Assist<String> chat) {
        this.messages = chat.messages();
    }

    public ChatResponse(I_Assist<String> chat, I_Agent agent) {
        this(chat, agent, new SimpleBindings());
    }

    public ChatResponse(I_Assist<String> chat, I_Agent agent, Bindings meta) {
        this.messages = chat.messages();
        this.meta = meta == null ? new SimpleBindings() : new SimpleBindings(meta);
        this.meta.put(Facades.SELF, agent.getSelf().stringValue());
        this.meta.put(Facades.FOCUS, agent.getStateMachine().getState().stringValue());
        Collection<Resource> intents = agent.getStateMachine().getTransitions();
        this.meta.put(Facades.INTENTS, Facts.toString(intents));
    }

    public Response build() {
        Response.ResponseBuilder builder = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(this);
        return addCORS(builder).build();
    }
}
