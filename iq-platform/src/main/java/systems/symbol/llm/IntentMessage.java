package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.I_Facade;
import systems.symbol.decide.I_Decide;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.I_Self;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

import static systems.symbol.string.Extract.extractIntent;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class IntentMessage extends TextMessage implements I_Self, I_Facade, I_Delegate<IRI> {

private String intent;
private Bindings bindings = new SimpleBindings();

@JsonCreator
public IntentMessage(
   @JsonProperty("intent") String intent,
   @JsonProperty("role") String role,
   @JsonProperty("content") String content) {
this.type = MessageType.text;
this.intent = extractIntent(intent);
this.role = RoleType.valueOf(role);
this.content = content;
}

public IntentMessage(
@JsonProperty("intent") String intent,
@JsonProperty("role") I_LLMessage.RoleType role,
@JsonProperty("content") String content, Bindings bindings) {
this.type = MessageType.text;
this.intent = extractIntent(intent);
this.role = role;
this.content = content;
this.bindings = bindings;
}

public String getIntent() {
return intent;
}

public IRI getSelf() {
return getIntent().isEmpty()?null:Values.iri(getIntent());
}

public String toString() {
return "{role: "+role+", intent: "+intent+", content: "+content+"}";
}

@Override
public Bindings getBindings() {
return bindings;
}

@Override
public IRI intent() throws StateException {
return getSelf();
}
}
