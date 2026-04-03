package systems.symbol.policy.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import systems.symbol.kernel.policy.PolicyInput;

import java.util.Objects;
import java.util.stream.Collectors;

public class OPAInputMapper {

private static final ObjectMapper MAPPER = new ObjectMapper();

public ObjectNode map(PolicyInput input) {
Objects.requireNonNull(input, "input is required");

ObjectNode node = MAPPER.createObjectNode();
node.put("principal", input.principal().toString());
node.put("realm", input.realm().toString());
node.put("action", input.action().toString());
node.put("resource", input.resource().toString());

node.putPOJO("roles", input.roles().stream().map(Object::toString).collect(Collectors.toList()));
node.putPOJO("scopes", input.scopes().stream().map(Object::toString).collect(Collectors.toList()));
node.putPOJO("context", input.context());

return node;
}
}
