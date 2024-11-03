package systems.symbol.controller.trust;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.I_Agent;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.realm.Facts;
import systems.symbol.sigint.GeoLocate;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.Realms;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.PrettyString;
import systems.symbol.string.Validate;
import systems.symbol.trust.SimpleKeyStore;
import systems.symbol.trust.generate.JWTGen;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static systems.symbol.platform.IQ_NS.TRUSTS;

@Path("trust")
public class TokenAPI {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject
RealmPlatform realms;
@Context
RoutingContext routing;
@ConfigProperty(name = "iq.realm.token.duration", defaultValue = "600")
int tokenDuration;

@OPTIONS
@Path("{path : .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
log.info("preflight.trust: {} @ {}", path, info.getBaseUri());
return new DataResponse().build();
}

@GET
@Produces(MediaType.APPLICATION_JSON)
@Path("/key/{realm}")
public Response publicKey(@PathParam("realm") String _realm) throws Exception {
if (Validate.isMissing(_realm))
return new OopsResponse("ux.token.realm", Response.Status.BAD_REQUEST).build();
I_Realm realm = realms.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.token.realm", Response.Status.NOT_FOUND).build();
String pkcs8 = SimpleKeyStore.toPKCS8(realm.keys().getPublic());
return new SimpleResponse(pkcs8).build();
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("token/{realm}/{provider}")
public Response login(@PathParam("realm") String _realm, @PathParam("provider") String provider,
SimpleBindings params, @Context UriInfo info) throws SecretsException {
log.info("trust.login: {} -> {} @ {}", _realm, provider, params.keySet());
if (provider == null || provider.length() < 4) {
return new OopsResponse("ux.token.provider", Response.Status.BAD_REQUEST).build();
}
if (Validate.isMissing(_realm)) {
return new OopsResponse("ux.token.realm", Response.Status.BAD_REQUEST).build();
}
I_Realm realm = realms.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.token.realm." + _realm, Response.Status.NOT_FOUND).build();
// TODO: authenticate (subject is a user, subject known to issuer)
// TODO: authorize (subject known to audience)

Repository repo = realm.getRepository();
if (repo == null)
return new OopsResponse("ux.token.repository." + _realm, Response.Status.NOT_FOUND).build();
try (RepositoryConnection connection = repo.getConnection()) {
connection.begin();

IRI issuer = Values.iri(realm.getSelf().stringValue(), "trust/" + provider + "/");
_realm = realm.getSelf().stringValue();

Bindings bindings = new SimpleBindings(params);
URI requestUri = info.getRequestUri();
String baseUrl = requestUri.getScheme() + "://" + requestUri.getHost()
+ (requestUri.getPort() != -1 ? ":3000/" : "/");
bindings.put("host", baseUrl);
bindings.put("issuer", issuer);
bindings.put("provider", provider);
log.info("trust.issuer: {} x {} <-- {}", issuer, connection.size(), baseUrl);

AgentBuilder builder = new AgentBuilder(issuer, connection, bindings, realm.getSecrets());
builder.setThoughts(realm.getModel()).scripting().sparql(connection);
I_Agent agent = builder.agent();
agent.start();
IRI initial = Values.iri(issuer.stringValue() + "verify");
// agent.getStateMachine().setInitial(Values.iri(issuer.stringValue()+"verify"));

Resource state = agent.getStateMachine().transition(initial);
agent.stop();
Object identity = bindings.get("identity");
log.info("trust.identity: {} == {} @ {}", agent.getSelf(), identity, state);
if (identity == null)
return new OopsResponse("ux.token.identity", Response.Status.NOT_FOUND).build();
if (!identity.toString().startsWith(agent.getSelf().stringValue())
|| identity.toString().length() == agent.getSelf().stringValue().length())
return new OopsResponse("ux.token.fraud", Response.Status.INTERNAL_SERVER_ERROR).build();

IRI self = Values.iri(identity.toString());
Object human = bindings.get("human");
log.info("trust.human: {} == {}", human == null ? "ANON" : human, self);
if (human == null || human.toString().isEmpty())
return new OopsResponse("ux.token.human.name", Response.Status.NOT_FOUND).build();
boolean newUser = realms.getRealm(self) != null;
I_Realm myRealm = realms.getInstance().newRealm(self);
log.info("trust.realm: {}", myRealm.getSelf());

// RDFDump.dump(myRealm.getModel());
try (RepositoryConnection myConnection = myRealm.getRepository().getConnection()) {
myConnection.begin();
trusting(routing, builder.getGround(), agent.getThoughts(), agent.getSelf(), self);
myConnection.add(agent.getThoughts());
myConnection.commit();
}
String[] roles = newUser
? new String[] { "noob", _realm + "role:user", _realm + "role:" + provider }
: new String[] { _realm + "role:user", _realm + "role:" + provider };
List<String> aud = new ArrayList<>();
aud.add(realm.getSelf().stringValue());
aud.add(self.stringValue());
Iterable<IRI> trusts = Realms.trusts(builder.getGround(), self, new IRIs(), true);
trusts.forEach(trust -> aud.add(trust.stringValue()));

int duration = PrettyString.getenv("MY_IQ_JWT_DURATION", tokenDuration); // 10 mins
String[] _aud = aud.toArray(new String[0]);
String signedToken = Realms.tokenize(issuer, roles, self.stringValue(), human.toString(), _aud, realm,
duration);
SimpleResponse response = new SimpleResponse("access_token", signedToken);
connection.commit();
return response.build();
} catch (Exception e) {
if (e instanceof OopsException oops) {
log.warn("trust.oops: {} == {}", e.getMessage(), oops.getStatus(), e);
return new SimpleResponse(oops.getMessage(), oops.getStatus()).build();
}
log.error("trust.failed: {}", e.getMessage(), e);
return new OopsResponse("ux.token.oops", Response.Status.FORBIDDEN).build();
}
}

protected void trusting(RoutingContext routing, Model ground, Model thoughts, IRI issuer, IRI self)
throws Exception {
Iterable<Statement> statements = ground.getStatements(issuer, TRUSTS, self);
if (statements.iterator().hasNext())
return;
Iterable<IRI> trusts = Realms.trusts(ground, self, new IRIs(), true);
Facts.copy(ground, trusts, thoughts);
GeoLocate geo = new GeoLocate();
String ipv4 = routing.request().remoteAddress().host();
log.info("trust.ipv4: {}", ipv4);
try {
geo.locate(self, ipv4, thoughts);
} catch (Exception e) {
/* no op */ }
// RDFDump.dump(thoughts);
ground.add(issuer, TRUSTS, self);
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("refresh/{realm}")
public Response reissue(@PathParam("realm") String _realm, @HeaderParam("Authorization") String bearer)
throws SecretsException, OopsException {
I_Realm realm = realms.getRealm(_realm);
if (realm == null)
return new OopsResponse("ux.token.realm", Response.Status.NOT_FOUND).build();
DecodedJWT jwt = GuardedAPI.decode(bearer, realm);
if (jwt == null)
return new OopsResponse("ux.token.bearer", Response.Status.FORBIDDEN).build();
JWTGen jwtGen = new JWTGen();
Map<String, Claim> claims = jwt.getClaims();
if (claims == null)
return new OopsResponse("ux.token.claims", Response.Status.FORBIDDEN).build();
Claim aud_claims = claims.get("aud");
String[] aud = aud_claims.asArray(String.class);
if (aud == null || aud.length == 0)
return new OopsResponse("ux.token.aud", Response.Status.FORBIDDEN).build();
if (!Validate.contains(aud, realm.getSelf().stringValue()))
return new OopsResponse("ux.token.self", Response.Status.FORBIDDEN).build();

log.info("trust.refresh: {} -> {} -> {}", jwt.getSubject(), jwt.getIssuer(), aud);

JWTCreator.Builder generator = jwtGen.generate(jwt.getIssuer(), jwt.getSubject(), aud, 600);
generator.withArrayClaim("roles", jwt.getClaims().get("roles").asArray(String.class));
generator.withClaim("name", jwt.getClaim("name").asString());
String signedToken = jwtGen.sign(generator, realm.keys());
SimpleResponse response = new SimpleResponse("access_token", signedToken);
return response.build();
}

// private String tokenize(IRI issuer, String provider, String self, String
// name, String[] claims, I_Keys keys) throws SecretsException {
// String[] roles = { "user", provider };
// return Realms.tokenize(issuer, roles, self, name, claims, keys, 600); // 10
// mins
// }

}
