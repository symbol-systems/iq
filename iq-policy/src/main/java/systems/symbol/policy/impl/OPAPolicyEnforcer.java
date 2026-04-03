package systems.symbol.policy.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;
import systems.symbol.policy.util.OPAInputMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public class OPAPolicyEnforcer implements I_PolicyEnforcer {

private static final Logger log = LoggerFactory.getLogger(OPAPolicyEnforcer.class);
private static final IRI ENFORCER = PolicyVocab.resource("enforcer:opa");

private final URL opaUrl;
private final Duration timeout;
private final String failMode;
private final OPAInputMapper mapper;
private final ObjectMapper objectMapper;

public OPAPolicyEnforcer(String opaUrl, Duration timeout, String failMode) {
try {
this.opaUrl = new URL(Objects.requireNonNull(opaUrl, "opaUrl is required"));
} catch (Exception ex) {
throw new IllegalArgumentException("invalid opaUrl", ex);
}
this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
this.failMode = failMode == null ? "closed" : failMode.toLowerCase();
this.mapper = new OPAInputMapper();
this.objectMapper = new ObjectMapper();
}

@Override
public PolicyResult evaluate(PolicyInput input) {
if (input == null) {
return PolicyResult.deny(PolicyVocab.REASON_NULL_INPUT, ENFORCER);
}
try {
HttpURLConnection c = (HttpURLConnection) opaUrl.openConnection();
c.setRequestMethod("POST");
c.setConnectTimeout((int) timeout.toMillis());
c.setReadTimeout((int) timeout.toMillis());
c.setDoOutput(true);
c.setRequestProperty("Content-Type", "application/json");

ObjectNode body = objectMapper.createObjectNode();
body.set("input", mapper.map(input));
byte[] payload = objectMapper.writeValueAsBytes(body);
c.getOutputStream().write(payload);

int status = c.getResponseCode();
InputStream is = status < 400 ? c.getInputStream() : c.getErrorStream();
JsonNode response = objectMapper.readTree(is);

JsonNode result = response.path("result");
if (result.isObject() && result.has("allow")) {
boolean allow = result.path("allow").asBoolean(false);
return allow ? PolicyResult.allow(PolicyVocab.DECISION_ALLOW, ENFORCER) : PolicyResult.deny(PolicyVocab.DECISION_DENY, ENFORCER);
}
if (result.isBoolean()) {
return result.asBoolean() ? PolicyResult.allow(PolicyVocab.DECISION_ALLOW, ENFORCER) : PolicyResult.deny(PolicyVocab.DECISION_DENY, ENFORCER);
}
return handleFailMode("OPA response did not contain allow field");
} catch (Exception ex) {
log.warn("OPA policy check failed", ex);
return handleFailMode("OPA failure: " + ex.getMessage());
}
}

private PolicyResult handleFailMode(String reason) {
if ("open".equals(failMode)) {
return PolicyResult.allow(PolicyVocab.DECISION_ALLOW, ENFORCER);
}
return PolicyResult.deny(PolicyVocab.REASON_POLICY_EVALUATION_ERROR, ENFORCER);
}

@Override
public String name() {
return "opa";
}
}
