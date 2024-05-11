package systems.symbol.llm;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class LLMessageDeserializer extends StdDeserializer<I_LLMessage<String>> {

public LLMessageDeserializer() {
this(null);
}

public LLMessageDeserializer(Class<?> vc) {
super(vc);
}

@Override
public I_LLMessage<String> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
JsonNode node = jp.getCodec().readTree(jp);
String role = node.get("role").asText();
String content = node.get("content").asText();
return new TextMessage(role, content);
}
}
