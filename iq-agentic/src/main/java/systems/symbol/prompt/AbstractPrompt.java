package systems.symbol.prompt;

import com.github.jknack.handlebars.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Optional;

public abstract class AbstractPrompt<T> implements I_LLM<T> {
protected final Logger log = LoggerFactory.getLogger(getClass());
protected static Handlebars hbs = new Handlebars();
Bindings my;

public AbstractPrompt(Bindings my) {
this.my = my;
}

public String bind(String prompt) throws IOException {
return bind(prompt, my);
}

public String bind(String prompt, Bindings bindings) throws IOException {
log.info("prompt.bind: {} -> {}", prompt, bindings.keySet());
return hbs.compileInline(prompt).apply(bindings);
}

public abstract I_Assist<T> complete(I_Assist<T> chat) throws APIException, IOException;
}
