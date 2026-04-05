package systems.symbol.controller.platform;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.policy.I_PolicyEnforcer;
import systems.symbol.kernel.policy.PolicyInput;
import systems.symbol.kernel.policy.PolicyResult;
import systems.symbol.kernel.policy.PolicyVocab;
import systems.symbol.kernel.pipeline.KernelCallContext;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.trust.generate.JWTGen;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class PolicyRequestFilter implements ContainerRequestFilter {

private static final Logger log = LoggerFactory.getLogger(PolicyRequestFilter.class);

@Inject
I_PolicyEnforcer enforcer;

@Inject
RealmPlatform platform;

@ConfigProperty(name = "iq.policy.public-paths", defaultValue = "/health,/q/,/oauth/,/cluster/,/mcp,/mcp/,/trust/nonce,/trust/guest")
String publicPaths;

@Override
public void filter(ContainerRequestContext requestContext) {
String path = requestContext.getUriInfo().getPath();
if (isPublicPath(path)) {
return;
}

String auth = requestContext.getHeaderString("Authorization");
if (auth == null || !auth.toLowerCase().startsWith("bearer ")) {
requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
.header("WWW-Authenticate", "Bearer realm=\"IQ\"")
.entity("Missing or invalid Authorization header")
.build());
return;
}

String token = auth.substring(7).trim();
DecodedJWT jwt;
try {
if (platform.getRealms().isEmpty()) {
throw new IllegalStateException("no realm available");
}
var realm = platform.getRealm(platform.getRealms().iterator().next());
var keys = realm.keys();
JWTGen jwtGen = new JWTGen();
jwt = jwtGen.verify(keys, token);
} catch (Exception ex) {
requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
.header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
.entity("Invalid token")
.build());
return;
}

String principal = jwt.getSubject();
IRI realm = null;
try {
if (!platform.getRealms().isEmpty()) {
realm = platform.getRealm(platform.getRealms().iterator().next()).getSelf();
}
} catch (Exception e) {
log.debug("Unable to resolve realm for policy filter", e);
}

KernelCallContext ctx = new KernelCallContext();
ctx.set(KernelCallContext.KEY_PRINCIPAL, principal);
ctx.set(KernelCallContext.KEY_REALM, realm);
ctx.set(KernelCallContext.KEY_JWT, token);
ctx.set(KernelCallContext.KEY_ROLES, jwt.getClaim("roles").asList(String.class));
ctx.set("kernel.scopes", jwt.getClaim("scope").asString() == null ? Collections.emptyList() : Arrays.asList(jwt.getClaim("scope").asString().split(" ")));
ctx.set(KernelCallContext.KEY_AUTHORISED, true);

PolicyInput input;
try {
input = PolicyInput.from(ctx, PolicyVocab.ACTION_EXECUTE, PolicyVocab.resource(path));
} catch (Exception ex) {
requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
.entity("Policy input error")
.build());
return;
}

PolicyResult result = enforcer.evaluate(input);
if (!result.allowed()) {
String report = result.reasonIri() != null ? result.reasonIri().toString() : result.reason();
log.warn("Policy denied {} on {}: {}", principal, path, report);
requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
.entity(report)
.build());
return;
}

requestContext.setProperty("kernel.jwt", jwt);
requestContext.setProperty("kernel.principal", principal);
}

private boolean isPublicPath(String path) {
if (path == null || publicPaths == null || publicPaths.isBlank()) {
return false;
}
List<String> paths = Arrays.stream(publicPaths.split(","))
.map(String::trim)
.toList();
for (String candidate : paths) {
if (candidate.isBlank()) continue;
if (path.startsWith(candidate)) {
return true;
}
}
return false;
}
}
