package systems.symbol.platform;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import systems.symbol.kernel.pipeline.I_AccessPolicy;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.policy.core.AllowAllEnforcer;
import systems.symbol.policy.decorator.AuditingPolicyEnforcer;
import systems.symbol.policy.decorator.CachingPolicyEnforcer;
import systems.symbol.policy.impl.CompositePolicyEnforcer;
import systems.symbol.policy.decorator.CircuitBreakerEnforcer;
import systems.symbol.policy.specialized.DelegationEnforcer;
import systems.symbol.policy.core.DenyAllEnforcer;
import systems.symbol.policy.impl.GraphPolicyEnforcer;
import systems.symbol.policy.impl.OPAPolicyEnforcer;
import systems.symbol.policy.adapter.PolicyEnforcerAdapter;
import systems.symbol.policy.impl.RDFPolicyEnforcer;
import systems.symbol.policy.impl.RoleBasedEnforcer;
import systems.symbol.policy.specialized.ScopeEnforcer;
import systems.symbol.kernel.policy.PolicyVocab;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class PolicyProducer {

@ConfigProperty(name = "iq.policy.enforcer", defaultValue = "rdf")
String enforcerType;

@ConfigProperty(name = "iq.policy.admin-role", defaultValue = "urn:iq:role:admin")
String adminRole;

@ConfigProperty(name = "iq.policy.fail-mode", defaultValue = "closed")
String failMode;

@ConfigProperty(name = "iq.policy.opa-url", defaultValue = "")
String opaUrl;

@ConfigProperty(name = "iq.policy.scope", defaultValue = "")
String policyScope;

@ConfigProperty(name = "iq.policy.cache.enabled", defaultValue = "false")
boolean cacheEnabled;

@ConfigProperty(name = "iq.policy.audit.mode", defaultValue = "none")
String auditMode;

@Inject
RealmPlatform platform;

@Produces
@Singleton
public I_PolicyEnforcer policyEnforcer() {
IRI adminRoleIri = null;
try {
adminRoleIri = SimpleValueFactory.getInstance().createIRI(adminRole);
} catch (Exception ex) {
// ignore invalid admin role IRI
}

I_PolicyEnforcer base;
switch (enforcerType.toLowerCase()) {
case "allow-all":
base = new AllowAllEnforcer();
break;
case "deny-all":
base = new DenyAllEnforcer();
break;
case "composite":
try {
List<I_PolicyEnforcer> list = new ArrayList<>();
list.add(new RDFPolicyEnforcer(platform.getRealm(platform.getRealms().iterator().next()).getRepository(), adminRoleIri));
list.add(new DelegationEnforcer());
base = new CompositePolicyEnforcer(list);
} catch (Exception ex) {
base = new DenyAllEnforcer();
}
break;
case "rbac":
try {
base = new RoleBasedEnforcer(Set.of(PolicyVocab.role("admin")));
} catch (Exception ex) {
base = new DenyAllEnforcer();
}
break;
case "scope":
try {
Set<org.eclipse.rdf4j.model.IRI> scopes = policyScope == null || policyScope.isBlank() ? Set.of() : Set.of(PolicyVocab.scope(policyScope));
base = new ScopeEnforcer(scopes);
} catch (Exception ex) {
base = new DenyAllEnforcer();
}
break;
case "graph":
try {
base = new GraphPolicyEnforcer(platform.getRealm(platform.getRealms().iterator().next()).getRepository(), adminRoleIri);
} catch (Exception ex) {
base = new DenyAllEnforcer();
}
break;
case "opa":
try {
if (opaUrl == null || opaUrl.isBlank()) {
throw new IllegalArgumentException("opa.url must be configured for opa enforcer");
}
base = new OPAPolicyEnforcer(opaUrl, Duration.ofSeconds(2), failMode);
} catch (Exception ex) {
base = new DenyAllEnforcer();
}
break;
case "rdf":
default:
try {
base = new RDFPolicyEnforcer(platform.getRealm(platform.getRealms().iterator().next()).getRepository(), adminRoleIri);
} catch (Exception ex) {
base = new DenyAllEnforcer();
}
break;
}

I_PolicyEnforcer decorated = base;
if (cacheEnabled) {
decorated = new CachingPolicyEnforcer(decorated);
}
if (!"none".equalsIgnoreCase(auditMode)) {
decorated = new AuditingPolicyEnforcer(decorated);
}

return decorated;
}

@Produces
@Singleton
public I_AccessPolicy accessPolicy(I_PolicyEnforcer enforcer) {
return new PolicyEnforcerAdapter(enforcer);
}
}
