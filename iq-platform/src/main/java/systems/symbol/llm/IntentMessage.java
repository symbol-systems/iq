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

import javax.script.Bindings;
import javax.script.SimpleBindings;

import static systems.symbol.string.Extract.extractIntent;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class IntentMessage extends TextMessage implements I_Facade, I_Delegate<IRI> {

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
        this.intent = extractIntent(meta.get("intent"));
        this.role = RoleType.assistant;
        this.content = content();
        inferType();
    }

    public IntentMessage(String intent, Bindings meta) {
        this.meta = meta;
        this.intent = intent;
        this.role = RoleType.assistant;
        this.content = content();
        inferType();
    }

    private String content() {
        Object content = this.meta.get("content");
        if (content == null)
            return null;
        this.meta.remove("content");
        return String.valueOf(content);
    }

    void inferType() {
        this.type = this.intent == null || this.intent.isEmpty()
                ? this.meta == null ? MessageType.text : MessageType.JSON
                : MessageType.intent;
    }

    public String getIntent() {
        return intent;
    }

    public String toString() {
        return "{ role: " + role + ", intent: " + intent + ", content: " + content + ", meta: " + meta.keySet() + "}";
    }

    @Override
    @JsonProperty("meta")
    public Bindings getBindings() {
        return meta;
    }

    @Override
    public IRI intent() throws StateException {
        try {
            return intent == null ? null : Values.iri(intent);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
