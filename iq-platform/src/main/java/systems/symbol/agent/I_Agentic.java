package systems.symbol.agent;

import systems.symbol.llm.I_Chat;

import javax.script.Bindings;

public interface I_Agentic<T, R>{
Bindings getBindings();
I_Chat<T> getConversation();
}