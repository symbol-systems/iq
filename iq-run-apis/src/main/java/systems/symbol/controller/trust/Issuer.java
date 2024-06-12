package systems.symbol.controller.trust;

import com.auth0.jwt.JWTCreator;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.responses.I_Response;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.platform.APIPlatform;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.service.ScriptService;
import systems.symbol.trust.generate.JWTGen;

import java.io.IOException;
import java.security.KeyPair;

@Path("trust")
public class JWT {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject
APIPlatform platform;

@GET
@Produces(MediaType.APPLICATION_JSON)
@Path("publickey")
public Response publicKey() throws Exception {
KeyPair keys = platform.loadKeyPair();
String pkcs8 = JWTGen.toPKCS8(keys.getPublic());
return new SimpleResponse(pkcs8).asJSON();
}
@GET
@Produces(MediaType.APPLICATION_JSON)
@Path("issuer/{repo}")
public Response issueJWT(@PathParam("repo")String repoName,
  @QueryParam("sub") String subject, @QueryParam("aud") String audience, @QueryParam("redirect") String redirect
, @Context UriInfo uriInfo ) throws IOException {
if (redirect==null||redirect.isEmpty()) {
String baseUrl = uriInfo.getBaseUri().toString();
redirect = baseUrl + "callback/login";
}
if (subject==null || subject.length()<4) {
return new OopsResponse("api.trust.issuer#subject-missing", Response.Status.BAD_REQUEST).asJSON();
}
if (repoName==null || repoName.isEmpty()) {
return new OopsResponse("api.trust.issuer#repo-missing", Response.Status.BAD_REQUEST).asJSON();
}

if (audience==null||audience.isEmpty()) audience = subject;

// TODO: authenticate (subject is a user, subject known to issuer)
// TODO: authorize (subject known to audience)

Repository repo = platform.getRepository(repoName);
if (repo==null) {
return new OopsResponse("api.trust.issuer#repo-unknown-"+repoName, Response.Status.NOT_FOUND).asJSON();
}
try (RepositoryConnection connection = repo.getConnection()) {
IRI issuer = platform.getSelf();

JWTGen jwtGen = new JWTGen();
JWTCreator.Builder generator = jwtGen.generate(issuer.stringValue(), subject, audience, 600);

// TODO: lookup
generator.withClaim("fullName", "Anon");
generator.withArrayClaim("roles", new String[] { "user" });
String signedToken = jwtGen.sign(generator, platform.loadKeyPair());

redirect = redirect+"?token="+signedToken;
log.info("trust.login: {}", redirect);
return Response.status(Response.Status.TEMPORARY_REDIRECT)
.location(UriBuilder.fromUri(redirect).build())
.build();
} catch (Exception e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.trust.issuer#repo-problem", Response.Status.INTERNAL_SERVER_ERROR).asJSON();

}
}

/**
 * CORS pre-flight
 */
@OPTIONS
@Path("issuer/*")
@Produces(MediaType.APPLICATION_JSON)
public Response preflight() {

log.info("preflight");
return new SimpleResponse("").asJSON();
}

@POST
@Produces(MediaType.APPLICATION_JSON)
@Path("issuer/{repo}/{provider}")
public Response issueJWT(@PathParam("repo")String repoName,
 @PathParam("provider") String provider
, @Context UriInfo uriInfo ) throws IOException {
if (provider==null || provider.length()<4) {
return new OopsResponse("api.trust.issuer#subject-missing", Response.Status.BAD_REQUEST).asJSON();
}
if (repoName==null || repoName.isEmpty()) {
return new OopsResponse("api.trust.issuer#repo-missing", Response.Status.BAD_REQUEST).asJSON();
}
// TODO: authenticate (subject is a user, subject known to issuer)
// TODO: authorize (subject known to audience)

Repository repo = platform.getRepository(repoName);
if (repo==null) {
return new OopsResponse("api.trust.issuer#repo-unknown-"+repoName, Response.Status.NOT_FOUND).asJSON();
}
try (RepositoryConnection connection = repo.getConnection()) {

IRI issuer = Values.iri(platform.getSelf().stringValue(), repoName);
IRI script = Values.iri(platform.getSelf().stringValue(), provider);
log.info("trust.issuers: {} -> {}", issuer, script);
ScriptService service = new ScriptService(issuer, new LiveModel(connection));
service.execute(script);
log.info("trust.issued: {}", service.getBindings());
MyFacade.dump(service.getBindings(), System.out);

JWTGen jwtGen = new JWTGen();
JWTCreator.Builder generator = jwtGen.generate(issuer.stringValue(), script.stringValue(), script.stringValue(), 600);

// TODO: lookup
generator.withClaim("fullName", "Anon");
generator.withArrayClaim("roles", new String[] { "user" });
String signedToken = jwtGen.sign(generator, platform.loadKeyPair());

return new SimpleResponse(signedToken).asJSON();
} catch (Exception e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.trust.issuer#repo-problem", Response.Status.INTERNAL_SERVER_ERROR).asJSON();

}
}}

