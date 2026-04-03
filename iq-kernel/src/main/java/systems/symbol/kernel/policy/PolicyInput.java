package systems.symbol.kernel.policy;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.kernel.pipeline.KernelCallContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public record PolicyInput(
IRI principal,
IRI realm,
IRI action,
IRI resource,
Set<IRI> scopes,
Set<IRI> roles,
Map<String, Object> context
) {

public PolicyInput {
Objects.requireNonNull(principal, "principal mustn't be null");
Objects.requireNonNull(realm, "realm mustn't be null");
Objects.requireNonNull(action, "action mustn't be null");
Objects.requireNonNull(resource, "resource mustn't be null");
Objects.requireNonNull(scopes, "scopes mustn't be null");
Objects.requireNonNull(roles, "roles mustn't be null");
Objects.requireNonNull(context, "context mustn't be null");
}

public static PolicyInput from(KernelCallContext ctx, IRI action, IRI resource) {
if (ctx == null) {
throw new IllegalArgumentException("KernelCallContext is required");
}
String principal = ctx.principal();
if (principal == null || principal.isBlank()) {
throw new IllegalArgumentException("principal is required");
}

IRI realm = ctx.realm();
if (realm == null) {
throw new IllegalArgumentException("realm is required");
}

if (action == null) {
throw new IllegalArgumentException("action is required");
}

if (resource == null) {
throw new IllegalArgumentException("resource is required");
}

IRI principalIri = PolicyVocab.principal(principal);
Set<IRI> roleIris = toRoleIRIs(ctx.get(KernelCallContext.KEY_ROLES));
Set<IRI> scopeIris = toScopeIRIs(ctx.get("kernel.scopes"));

return new PolicyInput(
principalIri,
realm,
action,
resource,
scopeIris,
roleIris,
ctx.attributes()
);
}

private static Set<IRI> toRoleIRIs(Object rawRoles) {
if (rawRoles == null) {
return Collections.emptySet();
}
if (rawRoles instanceof Set<?> s) {
return s.stream()
.filter(String.class::isInstance)
.map(String.class::cast)
.map(PolicyVocab::role)
.collect(Collectors.toSet());
}
if (rawRoles instanceof List<?> l) {
return l.stream()
.filter(String.class::isInstance)
.map(String.class::cast)
.map(PolicyVocab::role)
.collect(Collectors.toSet());
}
throw new IllegalArgumentException("Unsupported role collection: " + rawRoles.getClass());
}

private static Set<IRI> toScopeIRIs(Object rawScopes) {
if (rawScopes == null) {
return Collections.emptySet();
}
if (rawScopes instanceof Set<?> s) {
return s.stream()
.filter(String.class::isInstance)
.map(String.class::cast)
.map(PolicyVocab::scope)
.collect(Collectors.toSet());
}
if (rawScopes instanceof List<?> l) {
return l.stream()
.filter(String.class::isInstance)
.map(String.class::cast)
.map(PolicyVocab::scope)
.collect(Collectors.toSet());
}
throw new IllegalArgumentException("Unsupported scopes collection: " + rawScopes.getClass());
}

@Override
public String toString() {
return new StringJoiner(", ", PolicyInput.class.getSimpleName() + "[", "]")
.add("principal=" + principal)
.add("realm=" + realm)
.add("action=" + action)
.add("resource=" + resource)
.add("scopes=" + scopes)
.add("roles=" + roles)
.toString();
}
}
