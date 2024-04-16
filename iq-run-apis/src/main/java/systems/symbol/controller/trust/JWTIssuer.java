package systems.symbol.controller.trust;

import systems.symbol.platform.APIPlatform;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.trust.generate.JWTGen;
import com.auth0.jwt.JWTCreator;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("trust")
public class JWTIssuer {
protected final Logger log = LoggerFactory.getLogger(getClass());
@Inject
APIPlatform platform;

private String formatDate(Date date) {
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
return dateFormat.format(date);
}

@GET
//@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("issuer/{repo}")
public Response issue(@PathParam("repo")String repoName,
  @QueryParam("sub") String subject, @QueryParam("aud") String audience, @QueryParam("redirect") String redirect) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
if (redirect==null||redirect.isEmpty()) {
redirect = "/callback/token";
}
if (subject==null || subject.toString().length()<8) {
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
String signedToken = jwtGen.sign(generator, platform.loadKeyPair());

redirect = redirect+"?token="+signedToken;
return Response.status(Response.Status.TEMPORARY_REDIRECT)
.location(UriBuilder.fromUri(redirect).build())
.build();
} catch (Exception e) {
log.error(e.getMessage(), e);
return new OopsResponse("api.trust.issuer#repo-problem", Response.Status.INTERNAL_SERVER_ERROR).asJSON();

}
}

}

