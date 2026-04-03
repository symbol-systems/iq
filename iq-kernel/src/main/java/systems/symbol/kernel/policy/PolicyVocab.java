package systems.symbol.kernel.policy;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Well-known IRIs for the policy vocabulary.
 * <p>
 * Aligns with IQ-wide IRI-first approach.
 */
public final class PolicyVocab {

private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

private PolicyVocab() {}

public static final String NS = "urn:iq:policy:";

public static final IRI ACTION_READ  = vf.createIRI(NS + "action:read");
public static final IRI ACTION_WRITE = vf.createIRI(NS + "action:write");
public static final IRI ACTION_EXECUTE   = vf.createIRI(NS + "action:execute");
public static final IRI ACTION_ADMIN = vf.createIRI(NS + "action:admin");
public static final IRI ACTION_DELETE= vf.createIRI(NS + "action:delete");
public static final IRI ACTION_LIST  = vf.createIRI(NS + "action:list");
public static final IRI ACTION_SUBSCRIBE = vf.createIRI(NS + "action:subscribe");
public static final IRI ACTION_DELEGATE  = vf.createIRI(NS + "action:delegate");

public static final IRI DECISION_ALLOW = vf.createIRI(NS + "decision:allow");
public static final IRI DECISION_DENY  = vf.createIRI(NS + "decision:deny");

public static final IRI REASON_NULL_INPUT = vf.createIRI(NS + "reason:null-input");
public static final IRI REASON_ANONYMOUS_PRINCIPAL = vf.createIRI(NS + "reason:anonymous-principal");
public static final IRI REASON_ADMIN_ROLE_OVERRIDE = vf.createIRI(NS + "reason:admin-role-override");
public static final IRI REASON_PRINCIPAL_BLOCKED_FROM_REALM = vf.createIRI(NS + "reason:principal-blocked-from-realm");
public static final IRI REASON_PRINCIPAL_HAS_ACCESS_TO_REALM = vf.createIRI(NS + "reason:principal-has-access-to-realm");
public static final IRI REASON_REALM_IS_PUBLIC = vf.createIRI(NS + "reason:realm-is-public");
public static final IRI REASON_NO_MATCHING_ALLOW_POLICY = vf.createIRI(NS + "reason:no-matching-allow-policy");
public static final IRI REASON_ALL_ENFORCERS_ABSTAINED = vf.createIRI(NS + "reason:all-enforcers-abstained");
public static final IRI REASON_COMPOSITE_ALL_ALLOW = vf.createIRI(NS + "reason:composite-all-allow");
public static final IRI REASON_NO_SCOPES_REQUIRED = vf.createIRI(NS + "reason:no-scopes-required");
public static final IRI REASON_PRINCIPAL_MISSING_REQUIRED_SCOPE = vf.createIRI(NS + "reason:principal-missing-required-scope");
public static final IRI REASON_NO_ROLES_REQUIRED = vf.createIRI(NS + "reason:no-roles-required");
public static final IRI REASON_PRINCIPAL_MISSING_REQUIRED_ROLE = vf.createIRI(NS + "reason:principal-missing-required-role");
public static final IRI REASON_BEFORE_ALLOWED_WINDOW = vf.createIRI(NS + "reason:before-allowed-time-window");
public static final IRI REASON_AFTER_ALLOWED_WINDOW = vf.createIRI(NS + "reason:after-allowed-time-window");
public static final IRI REASON_WITHIN_ALLOWED_WINDOW = vf.createIRI(NS + "reason:within-allowed-time-window");
public static final IRI REASON_POLICY_EVALUATION_ERROR = vf.createIRI(NS + "reason:policy-evaluation-error");
public static final IRI REASON_RATE_LIMIT_EXCEEDED = vf.createIRI(NS + "reason:rate-limit-exceeded");
public static final IRI REASON_WITHIN_RATE_LIMIT = vf.createIRI(NS + "reason:within-rate-limit");
public static final IRI REASON_DENY_ALL_POLICY = vf.createIRI(NS + "reason:deny-all-policy");

public static final IRI HAS_ACCESS_TO   = vf.createIRI("urn:iq:hasAccessTo");
public static final IRI IS_BLOCKED_FROM = vf.createIRI("urn:iq:isBlockedFrom");
public static final IRI IS_PUBLIC   = vf.createIRI("urn:iq:isPublic");
public static final IRI GRANTS_SCOPE= vf.createIRI(NS + "grantsScope");
public static final IRI REQUIRES_SCOPE  = vf.createIRI(NS + "requiresScope");

public static final IRI SCOPE_CHAT_READ = vf.createIRI(NS + "scope:chat.read");
public static final IRI SCOPE_CHAT_WRITE= vf.createIRI(NS + "scope:chat.write");
public static final IRI SCOPE_AGENT_TRIGGER = vf.createIRI(NS + "scope:agent.trigger");
public static final IRI SCOPE_SPARQL_SELECT = vf.createIRI(NS + "scope:sparql.select");
public static final IRI SCOPE_SPARQL_UPDATE = vf.createIRI(NS + "scope:sparql.update");
public static final IRI SCOPE_REALM_ADMIN   = vf.createIRI(NS + "scope:realm.admin");
public static final IRI SCOPE_CONNECTOR_EXECUTE = vf.createIRI(NS + "scope:connector.execute");

public static final IRI EVENT_POLICY_EVALUATED = vf.createIRI("urn:iq:event:policy:evaluated");
public static final IRI EVENT_POLICY_DENIED= vf.createIRI("urn:iq:event:policy:denied");
public static final IRI EVENT_POLICY_ESCALATED = vf.createIRI("urn:iq:event:policy:escalated");

public static IRI principal(String sub) {
if (sub == null || sub.isBlank()) {
throw new IllegalArgumentException("principal sub is required");
}
return vf.createIRI("urn:iq:principal:" + sub);
}

public static IRI resource(String path) {
if (path == null || path.isBlank()) {
throw new IllegalArgumentException("resource path is required");
}
return vf.createIRI("urn:iq:resource:" + path);
}

public static IRI connectorOp(String connector, String operation) {
if (connector == null || connector.isBlank()) {
throw new IllegalArgumentException("connector is required");
}
if (operation == null || operation.isBlank()) {
throw new IllegalArgumentException("operation is required");
}
return vf.createIRI("urn:iq:connector:" + connector + ":" + operation);
}

public static IRI role(String name) {
if (name == null || name.isBlank()) {
throw new IllegalArgumentException("role name is required");
}
return vf.createIRI("urn:iq:role:" + name);
}

public static IRI scope(String name) {
if (name == null || name.isBlank()) {
throw new IllegalArgumentException("scope name is required");
}
return vf.createIRI(NS + "scope:" + name);
}

public static IRI tenant(String id) {
if (id == null || id.isBlank()) {
throw new IllegalArgumentException("tenant id is required");
}
return vf.createIRI("urn:iq:tenant:" + id);
}
}
