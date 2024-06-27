package systems.symbol.agent;

import systems.symbol.llm.I_Assist;
import systems.symbol.platform.I_Self;

import javax.script.Bindings;

public interface I_Agentic<T> extends I_Self {
Bindings getBindings();
I_Assist<T> getConversation();
}