package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.I_Self;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class IntentMessage extends TextMessage implements I_Self {

    private String intent;

    @JsonCreator
    public IntentMessage(
                       @JsonProperty("intent") String intent,
                       @JsonProperty("role") String role,
                       @JsonProperty("content") String content) {
        this.type = MessageType.text;
        this.intent = intent;
        this.role = RoleType.valueOf(role);
        this.content = content;
    }

    public IntentMessage(
            @JsonProperty("intent") String intent,
            @JsonProperty("role") I_LLMessage.RoleType role,
            @JsonProperty("content") String content) {
        this.type = MessageType.text;
        this.intent = intent;
        this.role = role;
        this.content = content;
    }

    public String getIntent() {
        return intent;
    }

    public IRI getSelf() {
        return Values.iri(getIntent());
    }

    public String toString() {
        return "{role: "+role+", intent: "+intent+", content: "+content+"}";
    }
}
