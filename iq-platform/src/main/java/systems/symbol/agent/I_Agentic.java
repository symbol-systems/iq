package systems.symbol.agent;

import systems.symbol.llm.I_Assist;

import javax.script.Bindings;

public interface I_Agentic<T>{
Bindings getBindings();
I_Assist<T> getConversation();
}