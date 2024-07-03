package systems.symbol.llm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import systems.symbol.platform.I_Self;

import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonSerialize
public class IntentMessage extends TextMessage implements I_Self {
static String EXTRACT_REGEX = "([a-zA-Z0-9:/]+)";

private String intent;

@JsonCreator
public IntentMessage(
   @JsonProperty("intent") String intent,
   @JsonProperty("role") String role,
   @JsonProperty("content") String content) {
this.type = MessageType.text;
this.intent = extract(intent);
this.role = RoleType.valueOf(role);
this.content = content;
}

public IntentMessage(
@JsonProperty("intent") String intent,
@JsonProperty("role") I_LLMessage.RoleType role,
@JsonProperty("content") String content) {
this.type = MessageType.text;
this.intent = extract(intent);
this.role = role;
this.content = content;
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

public static String extract(String s) {
Pattern pattern = Pattern.compile(EXTRACT_REGEX);
Matcher matcher = pattern.matcher(s);
if (matcher.find() && matcher.group(1).indexOf(":")>0) {
return matcher.group(1);
}
return "";
}
}
