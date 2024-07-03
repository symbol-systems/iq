package systems.symbol.prompt;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Collection;

public class ChoicePrompt extends AbstractPrompt<String> {
    I_Agent agent;
    String wrapper;

    public ChoicePrompt(Bindings my, I_Agent agent, String wrapper) {
        this(my, agent);
        this.wrapper = wrapper;
    }

    public ChoicePrompt(Bindings my, I_Agent agent) {
        super(my);
        this.agent = agent;
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        String prompt = prompt(agent.getThoughts(), agent.getStateMachine().getTransitions());
        log.debug("prompt.choices: {}", prompt);
        if (!prompt.isEmpty()) chat.system(bind(prompt));
        return chat;
    }

    public String bind(String prompt, Bindings bindings) throws IOException {
        if (wrapper==null||wrapper.isEmpty()) return super.bind(prompt,bindings);
        log.debug("prompt.rebind: {} -> {}", prompt, bindings.keySet());
        SimpleBindings my = new SimpleBindings(bindings);
        my.put("prompt", prompt);
        return hbs.compileInline(wrapper).apply(my);
    }

    public static String prompt(Model facts, Collection<Resource> choices) throws IOException {
        if (choices.isEmpty()) return "";
        StringBuilder intent$ = new StringBuilder();
        choices.forEach( c -> {
            Iterable<Statement> options = facts.getStatements(c, RDFS.LABEL, null);
            options.forEach( o -> intent$.append("\n- [").append(o.getSubject().stringValue()).append("](").append(o.getObject().stringValue()).append(")"));
        });
        return intent$.toString();
    }

    protected String prompt(Iterable<IRI> found) {
        StringBuilder p$ = new StringBuilder();
        found.forEach( c -> {
            Iterable<Statement> options = agent.getThoughts().getStatements(c, RDFS.LABEL, null);
            options.forEach( o -> p$.append("[").append(o.getSubject().stringValue()).append("](").append(o.getObject().stringValue()).append("), "));
        });
        return p$.toString();
    }
}
