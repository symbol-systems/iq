package systems.symbol.llm;

import systems.symbol.tools.APIException;

import java.io.IOException;

public interface I_LLM<T> {
    // I_LLMConfig getConfig();
    I_Assist<T> complete(I_Assist<T> chat) throws APIException, IOException;

    // boolean isOnline();

}
