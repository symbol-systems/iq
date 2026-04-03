package systems.symbol.kernel.policy;

import org.eclipse.rdf4j.model.IRI;

import java.time.Instant;
import java.util.Objects;

public record PolicyResult(
boolean allowed,
String reason,
IRI reasonIri,
IRI matchedRule,
Instant evaluatedAt
) {

public static PolicyResult allow(String reason, IRI matchedRule) {
return new PolicyResult(true,
reason == null ? "allowed" : reason,
null,
matchedRule,
Instant.now());
}

public static PolicyResult allow(IRI reasonIri, IRI matchedRule) {
Objects.requireNonNull(reasonIri, "reasonIri is required");
return new PolicyResult(true,
reasonIri.toString(),
reasonIri,
matchedRule,
Instant.now());
}

public static PolicyResult deny(String reason, IRI matchedRule) {
return new PolicyResult(false,
reason == null ? "denied" : reason,
null,
matchedRule,
Instant.now());
}

public static PolicyResult deny(IRI reasonIri, IRI matchedRule) {
Objects.requireNonNull(reasonIri, "reasonIri is required");
return new PolicyResult(false,
reasonIri.toString(),
reasonIri,
matchedRule,
Instant.now());
}

public static PolicyResult deny(String reason) {
return deny(reason, null);
}

public static PolicyResult abstain(String reason) {
return new PolicyResult(false,
reason == null ? "abstain" : reason,
null,
null,
Instant.now());
}

public PolicyResult {
Objects.requireNonNull(reason, "reason must not be null");
Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
}

public boolean isAbstain() {
return !allowed && matchedRule == null && "abstain".equals(reason);
}
}
