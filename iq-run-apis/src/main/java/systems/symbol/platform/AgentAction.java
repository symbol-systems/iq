package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.I_Facade;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;

import javax.script.SimpleBindings;
import java.util.ArrayList;
import java.util.Collection;

public class AgentAction implements I_Facade, I_Delegate<IRI> {
public String actor;
public String intent;
public SimpleBindings state;
public Collection<Resource> next;

public AgentAction() {
}

public AgentAction(String actor, String intent, SimpleBindings my) {
this.actor = actor;
this.intent = intent;
this.state = my;
this.next = new ArrayList<>();
}

public AgentAction(IRI actor, Resource intent, Collection<Resource> next, SimpleBindings my) {
this.actor = actor.stringValue();
this.intent = intent.stringValue();
this.state = my;
this.next = next;
}

@Override
public IRI intent() throws StateException {
return intent == null?null: Values.iri(intent);
}

public IRI getActor() {
return actor == null?null: Values.iri(actor);
}

//public Resource getIntent() {
//return intent == null?null: Values.iri(intent);
//}
//
public SimpleBindings getBindings() {
return state==null?new SimpleBindings():state;
}
}
