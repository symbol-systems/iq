package systems.symbol.agent;

import systems.symbol.llm.I_Assist;
import systems.symbol.platform.I_Self;

public interface I_Agentic<T> extends I_Self, I_Facade {
I_Assist<T> getConversation();
}