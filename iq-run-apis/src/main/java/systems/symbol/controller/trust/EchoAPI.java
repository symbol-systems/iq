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
import systems.symbol.agent.I_Agent;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.fsm.StateException;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.prompt.AgentPrompt;
import systems.symbol.prompt.PromptChain;
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
@Tag(name = "api.trust.chat.name", description = "api.trust.chat.description")
@Path("trust/chat")
public class EchoAPI extends GuardedAPI {

    /**
     * Endpoint for answering queries using LLM Language Models.
     *
     * @return JSON response containing language model results.
     */
    @POST
    @Operation(
            summary = "api.trust.chat.post.summary",
            description = "api.trust.chat.post.description"
    )
    @Path("{realm}/{actor:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(@PathParam("realm") String _realm, @PathParam("actor") String _actor, @HeaderParam("Authorization") String auth,
                               Conversation chat) throws APIException, IOException, SecretsException, PlatformException {
        Stopwatch stopwatch = new Stopwatch();
        log.info("trust.chat: {}", chat.messages());
        if (chat.messages().isEmpty()) return new OopsResponse("api.trust.chat.empty", Response.Status.NOT_FOUND).asJSON();
        if (Validate.isNonAlphanumeric(_realm)) return new OopsResponse("api.trust.chat.repository", Response.Status.BAD_REQUEST).asJSON();
        if (Validate.isMissing(_actor)) return new OopsResponse("api.trust.chat.missing", Response.Status.BAD_REQUEST).asJSON();
        IRI actor = Values.iri(_actor);
        I_Realm realm = platform.getRealm(Values.iri(_realm+":"));
        if (realm==null) return new OopsResponse("api.trust.chat.realm.missing", Response.Status.NOT_FOUND).asJSON();
        DecodedJWT jwt;
        try { jwt = authenticate(auth, realm); } catch (OopsException e) { return new OopsResponse(e.getMessage(), e.getStatus()).asJSON(); }
        Repository repository = realm.getRepository();
        if (repository == null) return new OopsResponse("api.trust.chat.repository.missing", Response.Status.NOT_FOUND).asJSON();

        Bindings bindings = new SimpleBindings();
        I_Realm myRealm = platform.getRealm(Values.iri(jwt.getSubject()));
        log.info("trust.chat.realm: {} @ {} & {}", actor, realm.getSelf(), myRealm.getSelf());

        try (RepositoryConnection connection = repository.getConnection()) {
            AgentBuilder builder = new AgentBuilder(actor, connection, bindings, realm.getSecrets());
            builder.self(chat).scripting();
            I_Agent agent = builder.avatar(chat);
            agent.start();
            PromptChain ai = new PromptChain();
            ai.add(new AgentPrompt(bindings, agent, realm.getModel()));
            I_Assist<String> complete = ai.complete(chat);
            log.info("trust.chat.reply: {} @ {}", complete.messages(), stopwatch);
            agent.stop();
            return new ChatResponse(complete).asJSON();
        } catch (StateException e) {
            return new OopsResponse("api.trust.chat.state", Response.Status.BAD_REQUEST).asJSON();
        } catch (Exception e) {
            return new OopsResponse("api.trust.chat.oops", Response.Status.INTERNAL_SERVER_ERROR).asJSON();
        }
    }
}
