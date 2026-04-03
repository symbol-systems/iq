package systems.symbol.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class JsonOutputSchema implements I_OutputSchema {

private final ObjectMapper mapper = new ObjectMapper();
private final List<String> requiredFields;

public JsonOutputSchema() {
this.requiredFields = new ArrayList<>();
}

public JsonOutputSchema(List<String> requiredFields) {
this.requiredFields = requiredFields != null ? requiredFields : new ArrayList<>();
}

@Override
public List<String> validate(String content) {
List<String> errors = new ArrayList<>();
if (content == null || content.isBlank()) {
errors.add("content is empty");
return errors;
}

try {
JsonNode root = mapper.readTree(content);
for (String field : requiredFields) {
if (!root.has(field)) {
errors.add("missing required field: " + field);
}
}
} catch (Exception e) {
errors.add("invalid json: " + e.getMessage());
}
return errors;
}
}
