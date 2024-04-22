package systems.symbol.agent;

import systems.symbol.llm.I_Thread;

import javax.script.Bindings;

public interface I_AgentContext<T, R>{
    Bindings getBindings();
    I_Thread<T> getConversation();
}