package systems.symbol.prompt;

import systems.symbol.tools.APIException;
import systems.symbol.agent.Facades;
import systems.symbol.agent.I_Agent;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLMessage;

import javax.script.Bindings;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.IOException;
import java.util.Optional;

public class AvatarPrompt extends AbstractPrompt<String> {
I_Assist<java.lang.String> ai = new Conversation();
Model facts;
I_Agent agent;

public AvatarPrompt(I_Agent agent, Model facts, Bindings my) {
super(Facades.rebind(agent.getSelf(), my));
this.agent = agent;
this.facts = facts;
}

public void avatar(Resource llm) throws IOException {
Bindings my = (Bindings) bindings.get(Facades.MY);

IRI actor = agent.getSelf();
my.put(Facades.AI, actor.getLocalName());

Optional<Literal> wrapper = Models.getPropertyLiteral(facts, llm, RDF.VALUE);
if (wrapper.isEmpty()) {
log.warn("prompt.avatar.llm: {} @ {}", actor, llm);
return;
}
String llm$ = wrapper.get().stringValue().trim();
if (llm$.isEmpty()) {
llm$ = "{{my.prompt}}";
}
String system$ = value(agent.getSelf()).trim();
if (system$.isEmpty()) {
log.warn("prompt.avatar.system: {} @ {}", actor, llm);
} else {
my.put("prompt", system$);
ai.system(interpolate(llm$));
}
}

public void assistant() throws IOException {
assistant(agent.getStateMachine().getState());
}

public void assistant(Resource state) throws IOException {
String assistant$ = interpolate(value(state).trim());
if (!assistant$.isEmpty())
ai.assistant(assistant$);
log.info("prompt.avatar.state: {} -> {}", state, assistant$);
}

public String value(Resource state) {
return value(state, RDF.VALUE);
}

public String value(Resource state, IRI predicate) {
if (state == null)
return null;
Optional<Literal> found = Models.getPropertyLiteral(facts, state, predicate);
return found.orElse(Values.***REMOVED***("")).stringValue();
}

@Override
public I_Assist<String> complete(I_Assist<String> chat) throws APIException, IOException {
for (I_LLMessage<String> m : chat.messages()) {
if (!m.getRole().equals(I_LLMessage.RoleType.system))
ai.add(m);
}

log.info("prompt.avatar.complete: {}", ai);
return ai;
}
}
