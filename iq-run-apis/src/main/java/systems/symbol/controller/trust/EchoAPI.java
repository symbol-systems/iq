package systems.symbol.controller.trust;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.Avatar;
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.Conversation;
import systems.symbol.realm.I_Realm;
import systems.symbol.realm.PlatformException;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;

/**
 * REST-ful API for Large Language Model (LLM) endpoints.
 */
@Tag(name = "api.trust.echo.name", description = "api.trust.echo.description")
@Path("trust/echo")
public class EchoAPI extends GuardedAPI {

    /**
     * Endpoint for answering queries using LLM Language Models.
     *
     * @return JSON response containing language model results.
     */
    @POST
    @Operation(summary = "api.trust.echo.post.summary", description = "api.trust.echo.post.description")
    @Path("{realm}/{actor:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(@PathParam("realm") String _realm, @PathParam("actor") String _actor,
            @HeaderParam("Authorization") String auth,
            Conversation chat) throws APIException, IOException, SecretsException, PlatformException {
        Stopwatch stopwatch = new Stopwatch();
        log.info("trust.chat: {}", chat.messages());
        if (chat.messages().isEmpty())
            return new OopsResponse("ux.trust.echo.empty", Response.Status.NOT_FOUND).build();
        if (Validate.isNonAlphanumeric(_realm))
            return new OopsResponse("ux.trust.echo.repository", Response.Status.BAD_REQUEST).build();
        if (Validate.isMissing(_actor))
            return new OopsResponse("ux.trust.echo.missing", Response.Status.BAD_REQUEST).build();
        IRI actor = Values.iri(_actor);
        I_Realm realm = platform.getRealm(Values.iri(_realm + ":"));
        if (realm == null)
            return new OopsResponse("ux.trust.echo.realm.missing", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }
        Repository repository = realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.trust.echo.repository.missing", Response.Status.NOT_FOUND).build();

        Bindings bindings = new SimpleBindings();
        I_Realm myRealm = platform.getRealm(Values.iri(jwt.getSubject()));
        log.info("trust.echo.realm: {} @ {} & {}", actor, realm.getSelf(), myRealm.getSelf());

        try (RepositoryConnection connection = repository.getConnection()) {
            AgentBuilder builder = new AgentBuilder(actor, connection, bindings, realm.getSecrets());
            builder.self(chat).scripting();
            Avatar avatar = builder.avatar(chat);
            // execute agentic LLM without state transition/actions
            avatar.execute(builder.getBindings());
            log.info("trust.echo.reply: {} @ {}", chat.messages().getLast(), stopwatch);
            return new ChatResponse(chat).build();
        } catch (StateException e) {
            return new OopsResponse("ux.trust.echo.state", Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return new OopsResponse("ux.trust.echo.oops", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
