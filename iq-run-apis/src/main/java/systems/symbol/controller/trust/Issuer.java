package systems.symbol.controller.trust;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.platform.APIPlatform;
import systems.symbol.platform.AgentService;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.SimpleKeyStore;
import systems.symbol.trust.generate.JWTGen;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.security.KeyPair;
import java.util.Collection;
import java.util.Map;

@Path("trust")
public class Issuer {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject
APIPlatform platform;

@OPTIONS
@Path("{path : .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight(@PathParam("path") String path) {
log.warn("preflight: {}", path);
return new DataResponse(null).asJSON();
}


@GET
@Produces(MediaType.APPLICATION_JSON)
@Path("publickey")
public Response publicKey() throws Exception {
KeyPair keys = platform.keys();
String pkcs8 = SimpleKeyStore.toPKCS8(keys.getPublic());
return new SimpleResponse(pkcs8).asJSON();
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("issuer/{repo}/{provider}")
public Response issue(@PathParam("repo") String repoName,
 @PathParam("provider") String provider
, SimpleBindings params) throws IOException {
log.info("trust.issuer: {} -> {} @ {}", repoName, provider, params.keySet());

if (provider == null || provider.length() < 4) {
return new OopsResponse("api.trust.issuer.subject-missing", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isMissing(repoName)) {
return new OopsResponse("api.trust.issuer.repo-missing", Response.Status.BAD_REQUEST).asJSON();
}
// TODO: authenticate (subject is a user, subject known to issuer)
// TODO: authorize (subject known to audience)

Repository repo = platform.getRepository(repoName);
if (repo == null) {
return new OopsResponse("api.trust.issuer.repo-unknown-" + repoName, Response.Status.NOT_FOUND).asJSON();
}
try (RepositoryConnection connection = repo.getConnection()) {

IRI issuer = Values.iri(platform.getSelf().stringValue(), repoName + ":trust:" + provider+":");
IRI state = Values.iri(issuer.stringValue(), "verify");
log.info("trust.intent: {} -> {}", issuer, state);
//MyFacade.dump(params, System.out);

Bindings bindings = new SimpleBindings(params);
bindings.put("issuer", issuer);
bindings.put("provider", provider);

AgentService service = new AgentService(issuer, connection, platform.getSecrets(), bindings);

if (service.getAgent().getStateMachine().getState() == null) {
return new OopsResponse("api.trust.issuer.state-unknown-" + repoName, Response.Status.NOT_FOUND).asJSON();
}
log.info("trust.issuers: {} -> {} @ {}", issuer, state, service.getAgent().getStateMachine().getState());

Resource done = service.next(state);
Map<?, Object> identity = (Map<?, Object>) bindings.getOrDefault("identity", null);
log.info("trust.identity: {} -> {} == {}", identity, !state.equals(done), service.getAgent().getStateMachine().getState());

//RDFDump.dump(new LiveModel(connection));
if (done == null || identity == null) {
return new OopsResponse("api.trust.issuer.denied-" + repoName, Response.Status.FORBIDDEN).asJSON();
}

String self = identity.getOrDefault("self", "urn:my:anonymous:" + provider).toString();
String name = identity.getOrDefault("name", "stranger").toString();
log.info("trust.tokenize: {} -> {}", identity, self);
String signedToken = tokenize(issuer, provider, self, name, new String[]{self, repoName, provider}, platform);
SimpleResponse response = new SimpleResponse("access_token", signedToken);
return response.asJSON();
} catch (Exception e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.trust.issuer.oops", Response.Status.FORBIDDEN).asJSON();
}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("refresh")
public Response reissue(@HeaderParam("Authorization") String bearer) throws SecretsException, OopsException {
DecodedJWT jwt = GuardedAPI.decode(bearer, platform);
if (jwt==null) {
return new OopsResponse("api.trust.issuer.token-invalid", Response.Status.FORBIDDEN).asJSON();
}

JWTGen jwtGen = new JWTGen();
Claim claims = jwt.getClaims().get("aud");
String[] aud = claims.asArray(String.class);
log.info("trust.refresh: {} -> {} -> {}", jwt.getSubject(), jwt.getIssuer(), aud);

JWTCreator.Builder generator = jwtGen.generate(jwt.getIssuer(), jwt.getSubject(), aud, 600);
generator.withArrayClaim("roles", jwt.getClaims().get("roles").asArray(String.class));
generator.withClaim("name", jwt.getClaim("name").asString());
String signedToken = jwtGen.sign(generator, platform.keys());
SimpleResponse response = new SimpleResponse("access_token", signedToken);
return response.asJSON();
}

private String tokenize(IRI issuer, String provider, String self, String name, String[] claims, I_Keys keys) throws SecretsException {
JWTGen jwtGen = new JWTGen();
JWTCreator.Builder generator = jwtGen.generate(issuer.stringValue(), self, claims, 600); // 10 mins
generator.withClaim("name", name);
generator.withArrayClaim("roles", new String[] { "user", provider });
return jwtGen.sign(generator, keys.keys());
}
}

