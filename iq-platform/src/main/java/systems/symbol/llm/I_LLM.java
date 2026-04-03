package systems.symbol.llm;

import systems.symbol.llm.tools.Tool;
import systems.symbol.tools.APIException;

import java.io.IOException;
import java.util.Collection;

public interface I_LLM<T> {
Collection<Tool> tools();

I_Assist<T> complete(I_Assist<T> chat) throws APIException, IOException;

default java.util.stream.Stream<I_LLMessage<T>> stream(I_Assist<T> chat) throws APIException, IOException {
I_Assist<T> result = complete(chat);
if (result == null || result.messages() == null) {
return java.util.stream.Stream.empty();
}
return result.messages().stream();
}

}
