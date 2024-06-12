package systems.symbol.controller.ux;

import com.auth0.jwt.JWTCreator;
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
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.platform.APIPlatform;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Map;

@Path("intent")
public class Intent {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    @Inject
    APIPlatform platform;

    /**
     * CORS pre-flight
     */
    @OPTIONS
    @Path("{path : .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response preflight() {
        log.info("preflight");
        return new DataResponse(null).asJSON();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{repo}/{resource: .*}")
    public Response postIntent(@PathParam("repo")String repoName,
                             SimpleBindings my
            , SimpleBindings params ) throws IOException {
        if (repoName==null || repoName.isEmpty()) {
            return new OopsResponse("api.ux.intent#repo-missing", Response.Status.BAD_REQUEST).asJSON();
        }
        Repository repo = platform.getRepository(repoName);
        if (repo==null) {
            return new OopsResponse("api.ux.intent#repo-unknown-"+repoName, Response.Status.NOT_FOUND).asJSON();
        }
        try (RepositoryConnection connection = repo.getConnection()) {

            String _agent = my.getOrDefault("self", null);
            IRI issuer = Values.iri(my.getOrDefault("actor", null));
            IRI state = Values.iri(issuer.stringValue(), ":verify");
            log.info("ux.intent.intent: {} -> {}", issuer, state);
//            MyFacade.dump(params, System.out);

            Bindings bindings = new SimpleBindings(params);
            bindings.put("issuer", issuer);
            bindings.put("resource", resource);

            APIPlatform.AgentService service = new APIPlatform.AgentService(issuer, connection, platform.getSecrets(), bindings);

            if (service.getAgent().getStateMachine().getState() == null) {
                return new OopsResponse("api.ux.intent#state-unknown-"+repoName, Response.Status.NOT_FOUND).asJSON();
            }
            log.info("ux.intent.issuers: {} -> {} @ {}", issuer, state, service.getAgent().getStateMachine().getState());

            Resource done = service.next(state);
            Map<?,Object> identity = (Map<?,Object>)bindings.getOrDefault("identity", null);
            log.info("ux.intent.identity: {} == {}", identity, service.getAgent().getStateMachine().getState());

//            RDFDump.dump(new LiveModel(connection));
            if (done==null || identity == null) {
                return new OopsResponse("api.ux.intent#denied-"+repoName, Response.Status.FORBIDDEN).asJSON();
            }

            JWTGen jwtGen = new JWTGen();
            String self = bindings.getOrDefault("self", "urn:anonymous").toString();
            JWTCreator.Builder generator = jwtGen.generate(issuer.stringValue(), state.stringValue(), self, 600);

            generator.withClaim("fullName", identity.getOrDefault("fullName", "").toString());
            generator.withArrayClaim("roles", new String[] { "user" });
            String signedToken = jwtGen.sign(generator, platform.loadKeyPair());

            SimpleResponse response = new SimpleResponse();
            response.set("access_code", signedToken);

            return response.asJSON();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new OopsResponse("api.ux.intent#oops", Response.Status.FORBIDDEN).asJSON();

        }
    }}

