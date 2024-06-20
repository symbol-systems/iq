package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;

import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.Collection;

public class AgentAction {
    public String agent;
    public String intent;
    public SimpleBindings state;
    public Collection<Resource> next;

    public AgentAction() {
    }

    public AgentAction(String agent, String intent, SimpleBindings my) {
        this.agent = agent;
        this.intent = intent;
        this.state = my;
        this.next = new ArrayList<>();
    }

    public AgentAction(IRI agent, Resource intent, Collection<Resource> next, SimpleBindings my) {
        this.agent = agent.stringValue();
        this.intent = intent.stringValue();
        this.state = my;
        this.next = next;
    }

    public IRI getAgent() {
        return agent == null?null: Values.iri(agent);
    }

    public Resource getIntent() {
        return intent == null?null: Values.iri(intent);
    }

    public SimpleBindings getBindings() {
        return state==null?new SimpleBindings():state;
    }
}
