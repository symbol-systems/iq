package systems.symbol.llm;

import systems.symbol.llm.tools.Tool;
import systems.symbol.tools.APIException;

import java.io.IOException;
import java.util.Collection;

public interface I_LLM<T> {
// I_LLMConfig getConfig();
Collection<Tool> tools();

I_Assist<T> complete(I_Assist<T> chat) throws APIException, IOException;

// boolean isOnline();

}
