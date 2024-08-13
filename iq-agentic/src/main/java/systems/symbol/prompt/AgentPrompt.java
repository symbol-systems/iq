package systems.symbol.prompt;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.llm.I_Assist;

import javax.script.Bindings;
import java.io.IOException;

public class AgentPrompt extends AbstractPrompt<String> {
    I_Agent agent;
    Model facts;

    public AgentPrompt(Bindings my, I_Agent agent, Model facts) {
        super(my);
        this.agent = agent;
        this.facts = facts;
    }

    public String prompt(IRI self, Resource state, Model facts) {
        return prompt(self, facts, RDF.VALUE)+prompt(state, facts, RDF.VALUE);
    }

    @Override
    public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
        String prompt = prompt(agent.getSelf(), agent.getStateMachine().getState(), facts);
        chat.system(bind(prompt));
        return chat;
    }

    protected String prompt(Iterable<IRI> found) {
        StringBuilder p$ = new StringBuilder();
        found.forEach( f -> { p$.append("[").append(f.getLocalName()).append("](").append(f.stringValue()).append("), "); });
        return p$.toString();
    }
}
