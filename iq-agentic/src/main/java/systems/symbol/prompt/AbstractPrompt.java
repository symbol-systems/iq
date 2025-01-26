package systems.symbol.prompt;

import com.github.jknack.handlebars.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.symbol.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.I_ToolSpec;
import systems.symbol.string.PrettyStrings;

import javax.script.Bindings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractPrompt<T> implements I_LLM<T> {
protected final Logger log = LoggerFactory.getLogger(getClass());
protected static Handlebars hbs = new Handlebars();
Bindings my;
private List<I_ToolSpec> tools = new ArrayList<>();

@Override
public Collection<I_ToolSpec> tools() {
return tools;
}

public AbstractPrompt(Bindings my) {
this.my = my;
}

public String bind(String prompt) throws IOException {
return bind(prompt, my);
}

public String bind(String prompt, Bindings bindings) throws IOException {
log.debug("prompt.bind: {} -> {}\n\n{}\n", prompt, bindings.keySet(), PrettyStrings.pretty(bindings));
return hbs.compileInline(prompt).apply(bindings);
}

public abstract I_Assist<T> complete(I_Assist<T> chat) throws APIException, IOException;
}
