package systems.symbol.agent;

import systems.symbol.llm.I_Assist;

import javax.script.Bindings;
import javax.script.SimpleBindings;

public class Agentic<T, R> implements I_Agentic<T> {
    Bindings bindings;
    I_Assist<T> chat;

    public Agentic(I_Assist<T> chat) {
        this.bindings = new SimpleBindings();
        this.chat = chat;
    }
    public Agentic(Bindings bindings, I_Assist<T> chat) {
        this.bindings = bindings;
        this.chat = chat;
    }
    @Override
    public Bindings getBindings() {
        return bindings;
    }

    @Override
    public I_Assist<T> getConversation() {
        return chat;
    }

}
