package systems.symbol.controller.trust;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
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
import systems.symbol.realm.Facts;
import systems.symbol.sigint.GeoLocate;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.Realms;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.trust.I_Keys;
import systems.symbol.trust.SimpleKeyStore;
import systems.symbol.trust.generate.JWTGen;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.net.URI;
import java.util.Set;

@Path("trust")
public class TokenAPI {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject RealmPlatform realms;
@Context
RoutingContext routing;

@OPTIONS
@Path("{path : .*}")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
log.debug("preflight.trust: {} @ {}", path, info.getBaseUri());
return new DataResponse().asJSON();
}


@GET
@Produces(MediaType.APPLICATION_JSON)
@Path("/key/{realm}")
public Response publicKey(@PathParam("realm") String _realm) throws Exception {
if (Validate.isMissing(_realm)) return new OopsResponse("api.token.realm", Response.Status.BAD_REQUEST).asJSON();
I_Realm realm = realms.getRealm(_realm);
if (realm == null) return new OopsResponse("api.token.realm", Response.Status.NOT_FOUND).asJSON();
String pkcs8 = SimpleKeyStore.toPKCS8(realm.keys().getPublic());
return new SimpleResponse(pkcs8).asJSON();
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("token/{realm}/{provider}")
public Response login(@PathParam("realm") String _realm, @PathParam("provider") String provider, SimpleBindings params, @Context UriInfo info) throws SecretsException {
log.info("trust.login: {} -> {} @ {}", _realm, provider, params.keySet());
if (provider == null || provider.length() < 4) {
return new OopsResponse("api.token.provider", Response.Status.BAD_REQUEST).asJSON();
}
if (Validate.isMissing(_realm)) {
return new OopsResponse("api.token.realm", Response.Status.BAD_REQUEST).asJSON();
}
I_Realm realm = realms.getRealm(_realm);
if (realm == null) return new OopsResponse("api.token.realm." + _realm, Response.Status.NOT_FOUND).asJSON();
// TODO: authenticate (subject is a user, subject known to issuer)
// TODO: authorize (subject known to audience)

Repository repo = realm.getRepository();
if (repo == null) return new OopsResponse("api.token.repository." + _realm, Response.Status.NOT_FOUND).asJSON();
try (RepositoryConnection connection = repo.getConnection()) {
connection.begin();

IRI issuer = Values.iri(realm.getSelf().stringValue(), "trust/" + provider+"/");
log.info("trust.issuer: {} x {}", issuer, connection.size());

Bindings bindings = new SimpleBindings(params);
URI requestUri = info.getRequestUri();
String baseUrl = requestUri.getScheme() + "://" + requestUri.getHost() + (requestUri.getPort() != -1 ? ":3000/" : "/");
bindings.put("host", baseUrl);
bindings.put("issuer", issuer);
bindings.put("provider", provider);

AgentBuilder builder = new AgentBuilder(issuer, bindings, realm.getSecrets());
builder.setGround(connection).setThoughts(realm.getModel()).executive().sparql(connection);
I_Agent agent = builder.build();
IRI state = Values.iri(issuer.stringValue()+"verify");
//agent.getStateMachine().setInitial(Values.iri(issuer.stringValue()+"verify"));

Resource done = agent.getStateMachine().transition(state);
log.info("trust.agent: {} -> {} & {} @ {}", agent.getSelf(), state ,done, baseUrl);
onboard(routing, builder.getGround(), agent.getThoughts(), agent.getSelf());

Object self = bindings.get("identity");
if (self==null) return new OopsResponse("api.token.self", Response.Status.NOT_FOUND).asJSON();
if (!self.toString().startsWith(agent.getSelf().stringValue()) || self.toString().length()==agent.getSelf().stringValue().length())
return new OopsResponse("api.token.self", Response.Status.INTERNAL_SERVER_ERROR).asJSON();

Object name = bindings.get("name");
if (name==null||name.toString().isEmpty()) return new OopsResponse("api.token.name", Response.Status.NOT_FOUND).asJSON();
log.info("trust.self: {} -> {} -> {} == {}", self, name, !state.equals(done), state);
I_Realm myRealm = realms.getRealm(self.toString());

RDFDump.dump(myRealm.getModel());
try (RepositoryConnection myConnection = myRealm.getRepository().getConnection()) {
myConnection.begin();
myConnection.add(agent.getThoughts());
myConnection.commit();
}
connection.commit();
String signedToken = tokenize(issuer, provider, self.toString(), name.toString(), new String[]{self.toString(), _realm, provider}, realm);
SimpleResponse response = new SimpleResponse("access_token", signedToken);
return response.asJSON();
} catch (Exception e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.token.oops", Response.Status.FORBIDDEN).asJSON();
}
}

protected void onboard(RoutingContext routing, Model ground, Model thoughts, IRI self) throws Exception {
Iterable<IRI> trusts = Realms.trusts(ground, self, new IRIs(), true);
Facts.copy(ground, trusts, thoughts);
GeoLocate geo = new GeoLocate();
String ipv4 = routing.request().remoteAddress().host();
log.info("trust.ipv4: {}", ipv4);
geo.locate(self, ipv4, thoughts);
//RDFDump.dump(thoughts);
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("refresh/{realm}")
public Response reissue(@PathParam("realm") String _realm, @HeaderParam("Authorization") String bearer) throws SecretsException, OopsException {
I_Realm realm = realms.getRealm(_realm);
DecodedJWT jwt = GuardedAPI.decode(bearer, realm);
if (jwt==null) {
return new OopsResponse("api.token.token", Response.Status.FORBIDDEN).asJSON();
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

