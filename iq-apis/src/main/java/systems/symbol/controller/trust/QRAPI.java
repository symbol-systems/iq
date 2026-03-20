package systems.symbol.controller.trust;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.controller.platform.RealmAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.platform.RealmPlatform;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.trust.generate.JWTGen;

import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

@Path("qr")
public class QRAPI extends RealmAPI {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    @Inject
    RealmPlatform realms;

    @OPTIONS
    @Path("{path : .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preflight(@PathParam("path") String path, @Context UriInfo info) {
        log.warn("qr.preflight: {} @ {}", path, info.getBaseUri());
        return new DataResponse(null).build();
    }

    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        return true;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{repo}/{code}")
    public Response invite(@PathParam("repo") String repoName, @PathParam("code") String code,
            @HeaderParam("Authorization") String bearer, @QueryParam("expiry") int expiry, @Context UriInfo info)
            throws IOException, OopsException, SecretsException {
        log.info("trust.qr: {} -> {} @ {}", repoName, code, info.getBaseUri());
        if (code == null || code.length() < 4)
            return new OopsResponse("ux.trust.qr.code", Response.Status.BAD_REQUEST).build();
        if (Validate.isMissing(repoName))
            return new OopsResponse("ux.trust.qr.realm", Response.Status.BAD_REQUEST).build();

        I_Realm realm = realms.getRealm(repoName);
        if (realm == null)
            return new OopsResponse("ux.trust.qr.realm", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(bearer, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }
        Repository repository = realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.trust.qr.repository", Response.Status.NOT_FOUND).build();

        try (RepositoryConnection connection = repository.getConnection()) {

            IRI self = Values.iri(info.getRequestUri().toString());
            log.info("trust.qr.url: {}", self);

            LDResponse ld = new LDResponse(RDFPrefixer.describe(connection, Values.iri(code)));
            JWTGen jwtGen = new JWTGen();
            if (expiry < 1 || expiry > 3600)
                expiry = 3600; // max/default 1 hour

            JWTCreator.Builder generator = jwtGen.generate(self.stringValue(), jwt.getSubject(),
                    new String[] { repoName, code }, expiry);
            generator.withClaim("@trust", ld.getBindings());
            log.info("@graph: {}", ld.getBindings().get("@graph"));

            String signed = jwtGen.sign(generator, realm.keys());
            log.info("trust.qr.seal: {}", signed);
            SimpleResponse response = new SimpleResponse("@trust", signed);
            return response.build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new OopsResponse("ux.trust.issuer.oops", Response.Status.FORBIDDEN).build();
        }
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{repo}")
    public Response learn(@PathParam("repo") String repoName, SimpleBindings bindings,
            @HeaderParam("Authorization") String bearer, @Context UriInfo info)
            throws IOException, OopsException, SecretsException {
        log.info("trust.qr.learn: {} -> {} -> {}", bindings.keySet(), info.getBaseUri(), bearer);
        I_Realm realm = realms.getRealm(repoName);
        if (realm == null)
            return new OopsResponse("ux.qr.learn.realm", Response.Status.NOT_FOUND).build();
        try {
            authenticate(bearer, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }
        Repository repository = realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.qr.learn.repository", Response.Status.NOT_FOUND).build();

        try (RepositoryConnection connection = repository.getConnection()) {
            Object qr = bindings.get("@trust");
            if (qr == null)
                return new OopsResponse("ux.qr.learn.trust", Response.Status.UNAVAILABLE_FOR_LEGAL_REASONS).build();
            JWTGen jwtGen = new JWTGen();
            DecodedJWT verified = jwtGen.verify(realm.keys(), qr.toString());

            if (verified == null)
                return new OopsResponse("ux.qr.learn.seal", Response.Status.UNAVAILABLE_FOR_LEGAL_REASONS).build();
            Claim claim = verified.getClaim("@trust");
            if (claim == null)
                return new OopsResponse("ux.qr.learn.claim", Response.Status.UNAVAILABLE_FOR_LEGAL_REASONS).build();
            Map<String, Object> trust = claim.asMap();
            String graph = trust.get("@graph").toString();
            if (graph == null)
                return new OopsResponse("ux.qr.learn.graph", Response.Status.UNAVAILABLE_FOR_LEGAL_REASONS).build();
            log.info("learn.qr.trust: {} -> {}", trust.keySet(), graph.getClass().getSimpleName());
            Model model = Rio.parse(new StringReader(graph), RDFFormat.JSONLD);
            if (model == null || model.isEmpty())
                return new OopsResponse("ux.qr.learn.null", Response.Status.NO_CONTENT).build();
            log.info("learn.qr.size: {}", model.size());
            connection.add(model);
            return new SimpleResponse().build();
        } catch (Exception e) {
            log.error("learn.qr.oops: {}", e.getMessage());
            return new OopsResponse("ux.trust.qr.oops", Response.Status.FORBIDDEN).build();
        }
    }

}
