package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.AgentBuilder;
import systems.symbol.agent.I_Agent;
import systems.symbol.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.finder.I_Corpus;
import systems.symbol.finder.I_Found;
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
import java.util.Collection;

/**
 * REST-ful API to chat with an LLM avatar.
 */
@Tag(name = "api.ux.chat.name", description = "api.ux.chat.description")
@Path("ux/chat")
public class ChatAPI extends GuardedAPI {
    @ConfigProperty(name = "iq.realm.chat.minScore", defaultValue = "0.2")
    double minScore;
    @ConfigProperty(name = "iq.realm.chat.maxResults", defaultValue = "5")
    int maxResults;

    /**
     * Endpoint for answering queries using LLM Language Models.
     *
     * @return JSON response containing language model results.
     */
    @POST
    @Operation(summary = "api ux chat post summary", description = "api.ux.chat.post.description")
    @Path("{realm}/{actor:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response chat(@PathParam("realm") String _realm, @PathParam("actor") String _actor,
            @HeaderParam("Authorization") String auth,
            @Context UriInfo uriInfo,
            Conversation chat) throws APIException, IOException, SecretsException, PlatformException {
        Stopwatch stopwatch = new Stopwatch();
        log.info("ux.chat: {}", chat.messages());
        if (Validate.isNonAlphanumeric(_realm))
            return new OopsResponse("ux.chat.repository", Response.Status.BAD_REQUEST).build();
        if (Validate.isMissing(_actor))
            return new OopsResponse("ux.chat.missing", Response.Status.BAD_REQUEST).build();
        IRI actor = Values.iri(_actor);
        I_Realm realm = platform.getRealm(Values.iri(_realm + ":"));
        if (realm == null)
            return new OopsResponse("ux.chat.realm.missing", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            log.info("ux.chat.token");
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }
        Repository realmRepository = realm.getRepository();
        if (realmRepository == null)
            return new OopsResponse("ux.chat.repository.missing", Response.Status.NOT_FOUND).build();

        Bindings bindings = new SimpleBindings();
        IRI user = Values.iri(jwt.getSubject());

        try (RepositoryConnection connection = realmRepository.getConnection()) {
            I_Realm myRealm = platform.getRealm(user);
            log.info("ux.chat.with: {} & {} == {}", actor, user, myRealm != null);
            if (myRealm == null)
                return new OopsResponse("ux.chat.realm.missing", Response.Status.NOT_FOUND).build();
            log.info("ux.chat.realm: {} @ {} & {} -> {}", actor, realm.getSelf(), myRealm.getSelf(), stopwatch);
            AgentBuilder builder = new AgentBuilder(actor, connection, bindings, realm.getSecrets()).scripting();
            builder.jwt(jwt).setThoughts(myRealm.getModel());

            bindings.put("realm", _realm);
            bindings.put("capacity", connection.size());
            I_Agent agent = builder.avatar(chat);

            // log.info("ux.chat.timer.1: {}", stopwatch.summary());
            Resource state = agent.getStateMachine().getState();
            if (state.isIRI()) {
                I_Corpus<IRI> searcher = platform.searcher(realm.getSelf());
                Collection<I_Found<IRI>> search = searcher.byConcept((IRI) state).search(chat.toString(), maxResults,
                        minScore);
                log.info("ux.chat.search: {} -> {} == {}", state, search.size(), chat.context());
                // log.info("ux.chat.timer.2: {}", stopwatch.summary());
                if ((search == null || search.isEmpty()) && state != null) {
                    search = searcher.byConcept(null).search(chat.context(), maxResults,
                            minScore);
                    log.info("ux.chat.search.2: {} -> {}", search.size(), chat.context());
                    // log.info("ux.chat.timer.3: {}", stopwatch.summary());
                }
                Collection<Resource> cando = agent.getStateMachine().getTransitions();
                final IRI[] _intent = new IRI[1];

                search.forEach(found -> {
                    try {
                        IRI intent = found.intent();
                        if (intent != null && cando.contains(intent)) {
                            _intent[0] = intent;

                        }
                    } catch (StateException e) {
                        log.error("Error processing intent: {}. State machine transition failed: {}", _intent[0],
                                e.getMessage(), e);
                        connection.rollback();
                    } catch (Exception e) {
                        log.error("Unexpected error: {}", e.getMessage(), e);
                    }
                });
                if (_intent[0] != null) {
                    synchronized (connection) {
                        agent.getStateMachine().setCurrentState(_intent[0]);
                        log.info("ux.chat.matrix.set: {} == {} <- {}", _intent[0], agent.getStateMachine().getState(),
                                cando);
                    }
                } else {
                    log.info("ux.chat.matrix.null: {}", state);
                }
            }
            agent.start();
            // log.info("ux.chat.timer.4: {}", stopwatch.summary());
            agent.stop();
            log.info("ux.chat.reply: {} = {} @ {}", agent.getThoughts().size(), chat.messages.getLast(), stopwatch);
            return new ChatResponse(chat, agent).build();
        } catch (StateException e) {
            log.error("ux.chat.oops: {}", e.getMessage());
            return new OopsResponse("ux.chat.state", Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("ux.chat.fatal: {}", e.getMessage(), e);
            return new OopsResponse("ux.chat.oops", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
