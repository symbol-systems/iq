package systems.symbol.prompt;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;
import systems.symbol.platform.IQ_NS;
import systems.symbol.realm.Facts;

import javax.script.Bindings;
import java.io.IOException;
import java.util.Set;

public class KnownsPrompt extends AbstractPrompt<String> {
    private final Model model;
    I_Agent agent;

    public KnownsPrompt(Bindings my, I_Agent agent, Model model) {
        super(my);
        this.agent = agent;
        this.model = model;
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        Set<IRI> iris = Facts.find(agent.getThoughts(), agent.getSelf(), IQ_NS.KNOWS);
        Facts.find(agent.getThoughts(), agent.getStateMachine().getState(), iris, false, IQ_NS.KNOWS);
        if (model!=null && !model.isEmpty() && !model.equals(agent.getThoughts())) {
            Facts.find(model, agent.getStateMachine().getState(), iris, false, IQ_NS.KNOWS);
            Facts.find(model, agent.getStateMachine().getState(), iris, false, IQ_NS.KNOWS);
        }
        chat.system(bind(prompt(iris)));
        return chat;
    }

    protected String prompt(Iterable<IRI> found) {
        StringBuilder p$ = new StringBuilder();
        found.forEach( f -> { p$.append("[").append(f.getLocalName()).append("](").append(f.stringValue()).append("), "); });
        return p$.toString();
    }
}
