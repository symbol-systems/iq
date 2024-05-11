package systems.symbol.agent;

import systems.symbol.llm.I_Chat;

import javax.script.Bindings;

public class AgentContext<T, R> implements I_AgentContext<T, R> {
    Bindings bindings;
    I_Chat<T> chat;


    public AgentContext(Bindings bindings, I_Chat<T> chat) {
        this.bindings = bindings;
        this.chat = chat;
    }
    @Override
    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public I_Chat<T> getConversation() {
        return chat;
    }

}
