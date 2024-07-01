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
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.I_Agent;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.SimpleKeyStore;
import systems.symbol.trust.generate.JWTGen;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Map;

@Path("trust")
public class IssuerAPI {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject RealmPlatform realms;

@OPTIONS
@Path("{path : .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
log.warn("preflight.trust: {} @ {}", path, info.getBaseUri());
return new DataResponse().asJSON();
}


@GET
@Produces(MediaType.APPLICATION_JSON)
@Path("/key/{realm}")
public Response publicKey(@PathParam("realm") String _realm) throws Exception {
if (Validate.isMissing(_realm)) return new OopsResponse("api.trust.realm", Response.Status.BAD_REQUEST).asJSON();
I_Realm realm = realms.getRealm(_realm);
if (realm == null) return new OopsResponse("api.trust.realm", Response.Status.NOT_FOUND).asJSON();
String pkcs8 = SimpleKeyStore.toPKCS8(realm.keys().getPublic());
return new SimpleResponse(pkcs8).asJSON();
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("issuer/{realm}/{provider}")
public Response login(@PathParam("realm") String _realm, @PathParam("provider") String provider, SimpleBindings params) throws SecretsException {
log.info("trust.login: {} -> {} @ {}", _realm, provider, params.keySet());
if (provider == null || provider.length() < 4) {
return new OopsResponse("api.trust.issuer.subject", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isMissing(_realm)) {
return new OopsResponse("api.trust.issuer.realm", Response.Status.BAD_REQUEST).asJSON();
}
I_Realm realm = realms.getRealm(_realm);
// TODO: authenticate (subject is a user, subject known to issuer)
// TODO: authorize (subject known to audience)

Repository repo = realm.getRepository();
if (repo == null) {
return new OopsResponse("api.trust.issuer.realm." + _realm, Response.Status.NOT_FOUND).asJSON();
}
try (RepositoryConnection connection = repo.getConnection()) {

IRI issuer = Values.iri(_realm, ":trust/" + provider+"/");
log.info("trust.issuer: {} x {}", issuer, connection.size());

Bindings bindings = new SimpleBindings(params);
bindings.put("issuer", issuer);
bindings.put("provider", provider);

AgentBuilder builder = new AgentBuilder(issuer, bindings, realm.getSecrets());
builder.setGround(connection);//.setThoughts(new LiveModel(connection));
builder.executive().sparql(connection);
I_Agent agent = builder.build();
Resource state = agent.getStateMachine().getState();
log.info("trust.agent: {} -> {}", agent.getSelf(), state);
if (state == null) {
return new OopsResponse("api.trust.issuer.state-unknown", Response.Status.NOT_FOUND).asJSON();
}
Resource done = agent.getStateMachine().transition(state);
Map<?, Object> identity = (Map<?, Object>) bindings.getOrDefault("identity", null);
log.info("trust.identity: {} -> {} == {}", identity, !state.equals(done), state);

//RDFDump.dump(new LiveModel(connection));
if (done == null || identity == null) {
return new OopsResponse("api.trust.issuer.denied-" + _realm, Response.Status.FORBIDDEN).asJSON();
}
String self = identity.getOrDefault("self", "anon:" + provider).toString();
String name = identity.getOrDefault("name", "anon").toString();
log.info("trust.tokenize: {} -> {} == {}", identity, self, name);

String signedToken = tokenize(issuer, provider, self, name, new String[]{self, _realm, provider}, realm);
SimpleResponse response = new SimpleResponse("access_token", signedToken);
return response.asJSON();
} catch (Exception e) {
log.error(e.getMessage());
return new OopsResponse("api.trust.issuer.oops", Response.Status.FORBIDDEN).asJSON();
}
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("refresh/{realm}")
public Response reissue(@PathParam("realm") String _realm, @HeaderParam("Authorization") String bearer) throws SecretsException, OopsException {
I_Realm realm = realms.getRealm(_realm);
DecodedJWT jwt = GuardedAPI.decode(bearer, realm);
if (jwt==null) {
return new OopsResponse("api.trust.issuer.token", Response.Status.FORBIDDEN).asJSON();
}

JWTGen jwtGen = new JWTGen();
Claim claims = jwt.getClaims().get("aud");
String[] aud = claims.asArray(String.class);
log.info("trust.refresh: {} -> {} -> {}", jwt.getSubject(), jwt.getIssuer(), aud);

JWTCreator.Builder generator = jwtGen.generate(jwt.getIssuer(), jwt.getSubject(), aud, 600);
generator.withArrayClaim("roles", jwt.getClaims().get("roles").asArray(String.class));
generator.withClaim("name", jwt.getClaim("name").asString());
String signedToken = jwtGen.sign(generator, realm.keys());
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

