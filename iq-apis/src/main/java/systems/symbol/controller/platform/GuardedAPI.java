package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;

public class GuardedAPI extends RealmAPI {

@Inject
I_PolicyEnforcer policyEnforcer;

@Override
public boolean entitled(DecodedJWT jwt, IRI agent) {
return jwt.getAudience().contains(agent.stringValue());
}

/**
 * Checks ACL enforcement for realm access.
 * Enforces: (principal, realm) ACL rules.
 *
 * @param principal The principal (JWT subject)
 * @param realm The realm being accessed
 * @param jwt   The decoded JWT token
 * @throws OopsException if principal is not authorized for realm
 */
public void checkRealmAccess(String principal, IRI realm, DecodedJWT jwt) throws OopsException {
if (policyEnforcer == null) {
log.warn("GuardedAPI: no policy enforcer configured; denying realm access");
throw new OopsException("ux.realm.unauthorized", Response.Status.FORBIDDEN);
}

var ctx = new systems.symbol.kernel.pipeline.KernelCallContext();
ctx.set(systems.symbol.kernel.pipeline.KernelCallContext.KEY_PRINCIPAL, principal);
ctx.set(systems.symbol.kernel.pipeline.KernelCallContext.KEY_REALM, realm);
ctx.set(systems.symbol.kernel.pipeline.KernelCallContext.KEY_JWT, jwt == null ? null : jwt.getToken());
if (jwt != null) {
ctx.set(systems.symbol.kernel.pipeline.KernelCallContext.KEY_ROLES, jwt.getClaim("roles").asList(String.class));
}

PolicyInput input;
try {
input = PolicyInput.from(ctx, PolicyVocab.ACTION_READ, realm);
} catch (Exception ex) {
log.warn("GuardedAPI: failed to build policy input", ex);
throw new OopsException("ux.realm.unauthorized", Response.Status.FORBIDDEN);
}

PolicyResult result = policyEnforcer.evaluate(input);
if (!result.allowed()) {
String report = result.reasonIri() != null ? result.reasonIri().toString() : result.reason();
log.warn("Policy denied realm access [{}] for principal {}: {}", realm, principal, report);
throw new OopsException("ux.realm.unauthorized", Response.Status.FORBIDDEN);
}

log.info("Policy allowed realm access [{}] for principal {}", realm, principal);
}
}
