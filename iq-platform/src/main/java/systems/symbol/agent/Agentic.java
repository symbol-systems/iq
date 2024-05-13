package systems.symbol.agent;

import systems.symbol.llm.I_Chat;

import javax.script.Bindings;
import javax.script.SimpleBindings;

public class Agentic<T, R> implements I_Agentic<T, R> {
    Bindings bindings;
    I_Chat<T> chat;

    public Agentic(I_Chat<T> chat) {
        this.bindings = new SimpleBindings();
        this.chat = chat;
    }
    public Agentic(Bindings bindings, I_Chat<T> chat) {
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
