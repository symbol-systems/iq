package systems.symbol.llm;

import systems.symbol.agent.tools.APIException;

import java.io.IOException;

public interface I_LLM<T> {
I_LLMConfig getConfig();
void complete(I_Assist<T> request) throws APIException, IOException;

boolean isOnline();

}
