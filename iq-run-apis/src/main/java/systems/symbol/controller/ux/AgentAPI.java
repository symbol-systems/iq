package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.Agentic;
import systems.symbol.agent.MyFacade;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.*;
import systems.symbol.decide.LLMDecision;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_Assist;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.Prompts;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.platform.AgentService;
import systems.symbol.platform.I_Self;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;
import systems.symbol.util.Stopwatch;

import javax.script.Bindings;
import javax.script.SimpleBindings;

@Tag(name = "api.ux.agent.name", description = "api.ux.agent.description")
@Path("ux/agent")
public class AgentAPI extends GuardedAPI {
//
//    @GET
//    @Operation(
//            summary = "api.ux.agent.get.summary",
//            description = "api.ux.agent.get.description"
//    )
//    @Produces("application/ld+json")
//    @Path("{repo}/{actor: .*}")
//    public Response about(@PathParam("repo") String repo,@PathParam("actor") String _agent, @HeaderParam("Authorization") String auth) throws Exception {
//        DecodedJWT jwt;
//        try {
//            jwt = authenticate(auth);
//        } catch (OopsException e) {
//            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
//        }
//        if (!Validate.isURN(_agent)) {
//            return new OopsResponse("api.ux.about#invalid", Response.Status.BAD_REQUEST).asJSON();
//        }
//        log.info("ux.about.agent: {} -> {}", _agent, jwt.getSubject());
//
//        IRI actor = Values.iri(_agent);
//        Repository repository = platform.getRepository(repo);
//        try (RepositoryConnection connection = repository.getConnection()) {
//            String sparql = RDFPrefixer.toSPARQL(connection, "DESCRIBE <" + actor + ">");
//            GraphQuery graphQuery = connection.prepareGraphQuery(sparql);
//            return new LDResponse(graphQuery).asJSON();
//        }
//    }

    @GET
    @Operation(
            summary = "api.ux.agent.get.summary",
            description = "api.ux.agent.get.description"
    )
    @Produces("application/ld+json")
    @Path("{repo}/{agent: .*}")
    public Response hello(@PathParam("repo") String repo,@PathParam("agent") String _agent, @HeaderParam("Authorization") String auth) throws Exception, APIException {
        Stopwatch stopwatch = new Stopwatch();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
        }
        if (!Validate.isURN(_agent)) {
            return new OopsResponse("api.ux.hello#invalid", Response.Status.BAD_REQUEST).asJSON();
        }

        I_Secrets secrets = platform.getSecrets();
        String llmToken = secrets.getSecret("MY_OPENAI_API_KEY");
        if (Validate.isMissing(llmToken)) {
            return new OopsResponse("api.ux.hello#disabled", Response.Status.BAD_REQUEST).asJSON();
        }
        IRI actor = Values.iri(_agent);
        Repository repository = platform.getRepository(repo);
        try (RepositoryConnection connection = repository.getConnection()) {
            Bindings my = MyFacade.rebind(actor, new SimpleBindings(), jwt);

            GenericGPT llm = new GenericGPT(llmToken, 1000);
            AgentService service = new AgentService(actor, connection, secrets, my);
            Resource state = service.getAgent().getStateMachine().getState();
            log.info("ux.hello.state: {} -> {} @ {}", repo, _agent, state);
            I_Assist<String> chat = Prompts.prompt(actor, state, new LiveModel(connection), my);
            llm.complete(chat);
            log.info("ux.hello.reply: {} -> {} @ {}", actor, chat.messages().size(), stopwatch);
            return new ChatResponse(chat).asJSON();
        }
    }

    @POST
    @Operation(
            summary = "api.ux.agent.post.summary",
            description = "api.ux.agent.post.description"
    )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/ld+json")
    @Path("{repo}/{agent: .*}")
    public Response ask(@PathParam("repo") String repo,@PathParam("agent") String _agent, @HeaderParam("Authorization") String auth, Conversation chat) throws Exception {
        log.info("ux.agent.ask: {} -> {} -> {}", repo, _agent, chat);
        Stopwatch stopwatch = new Stopwatch();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).asJSON();
        }
        if (!Validate.isURN(_agent)) {
            return new OopsResponse("api.ux.agent#invalid", Response.Status.BAD_REQUEST).asJSON();
        }

        I_Secrets secrets = platform.getSecrets();
        String llmToken = secrets.getSecret("MY_OPENAI_API_KEY");
        if (Validate.isMissing(llmToken)) {
            return new OopsResponse("api.ux.agent#disabled", Response.Status.BAD_REQUEST).asJSON();
        }
        IRI actor = Values.iri(_agent);
        Repository repository = platform.getRepository(repo);
        if (repository == null) {
            return new OopsResponse("api.ux.agent#repository-missing", Response.Status.NOT_FOUND).asJSON();
        }
        try (RepositoryConnection connection = repository.getConnection()) {
            Bindings my = MyFacade.rebind(actor, new SimpleBindings(), jwt);

            AgentService service = new AgentService(actor, connection, secrets, my);

            I_LLM<String> llm = new GenericGPT(llmToken, 1000);
            IRI self = Values.iri(jwt.getSubject());
            log.info("ux.agent.self: {}", self);
            Agentic<String, Resource> agentic = new Agentic<>(() -> self, my, chat);
            LLMDecision manager = new LLMDecision(llm, service.getAgent(), agentic);
            IRI decided = manager.decide();
            if (Validate.isMissing(decided)) {
                return new OopsResponse("api.ux.agent#decision", Response.Status.BAD_REQUEST).asJSON();
            }

            // FSM transition
            Resource state = service.getAgent().getStateMachine().transition(decided);
            log.info("ux.agent.decided: {} -> {}", decided, state);
            if (Validate.isMissing(state)) {
                return new OopsResponse("api.ux.agent#state", Response.Status.BAD_REQUEST).asJSON();
            }

            log.info("ux.agent.done: {} -> {} @ {}", actor, state, stopwatch);
            return new ChatResponse(chat).asJSON();
        }
    }
}
