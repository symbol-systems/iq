package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.agent.I_Facade;
import systems.symbol.decide.I_Delegate;
import systems.symbol.fsm.StateException;
import systems.symbol.platform.I_Self;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import static systems.symbol.string.Extract.extractIntent;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class IntentMessage extends TextMessage implements I_Self, I_Facade, I_Delegate<IRI> {

@JsonInclude(JsonInclude.Include.NON_NULL)
String intent;
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonProperty("meta")
Bindings meta = new SimpleBindings();

@JsonCreator
public IntentMessage(
   @JsonProperty("intent") String intent,
   @JsonProperty("role") RoleType role,
   @JsonProperty("content") String content) {
this.intent = extractIntent(intent);
this.role = role;
this.content = content;
inferType();
}

public IntentMessage(@JsonProperty("meta") Bindings meta) {
this.meta = meta;
this.intent = extractIntent(String.valueOf(meta.get("intent")));
this.role = RoleType.assistant;
this.content = String.valueOf(meta.get("content"));
this.meta.remove("intent");
this.meta.remove("content");
inferType();
}

void inferType() {
this.type = this.intent.isEmpty()?this.meta==null?MessageType.text:MessageType.JSON:MessageType.intent;
}

public String getIntent() {
return intent;
}

public IRI getSelf() {
return getIntent().isEmpty()?null:Values.iri(getIntent());
}

public String toString() {
return "{role: "+role+", intent: "+intent+", content: "+content+", meta: "+meta.keySet()+"}";
}

@Override
@JsonProperty("meta")
public Bindings getBindings() {
return meta;
}

@Override
public IRI intent() throws StateException {
return getSelf();
}
}
