package systems.symbol.controller.ux;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.MyFacade;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.DataResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.platform.AgentAction;
import systems.symbol.platform.AgentService;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.util.Collection;

@Path("ux/intent")
@Tag(name = "api.ux.intent.name", description = "api.ux.intent.description")
public class IntentAPI extends GuardedAPI {

    @POST
    @Operation(
            summary = "api.ux.intent.post.summary",
            description = "api.ux.intent.post.description"
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{realm}")
    public Response action(@PathParam("realm")String _realm, AgentAction action, @HeaderParam("Authorization") String auth) throws IOException, SecretsException {

        if (_realm==null || _realm.isEmpty()) return new OopsResponse("api.ux.intent.repo-missing", Response.Status.BAD_REQUEST).asJSON();
        I_Realm realm = platform.getRealm(_realm);
        if (realm==null) return new OopsResponse("api.ux.intent.realm", Response.Status.NOT_FOUND).asJSON();
        DecodedJWT jwt;
        try { jwt = authenticate(auth, realm); } catch (OopsException e) { return new OopsResponse(e.getMessage(), e.getStatus()).asJSON(); }
        Repository repo = realm.getRepository();
        if (repo==null) return new OopsResponse("api.ux.intent.repo-unknown-"+_realm, Response.Status.NOT_FOUND).asJSON();

        log.info("ux.intent.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());
        if (action==null) {
            return new OopsResponse("api.ux.intent.missing", Response.Status.BAD_REQUEST).asJSON();
        }
        log.info("ux.intent.action: {} => {}", action.getAgent(), action.getIntent());
        if (action.getAgent() ==null || action.getIntent() == null) {
            return new OopsResponse("api.ux.intent.invalid", Response.Status.BAD_REQUEST).asJSON();
        }

        try (RepositoryConnection connection = repo.getConnection()) {

            log.info("ux.intent.action: {} -> {}", repo, action);
//            MyFacade.dump(params, System.out);
            Bindings my = MyFacade.rebind(action.getAgent(), action.getBindings(), jwt);

            AgentService service = new AgentService(action.getAgent(), connection, realm.getSecrets(), my);

            if (service.getAgent() == null) {
                return new OopsResponse("api.ux.intent.agent-unknown", Response.Status.NOT_FOUND).asJSON();
            }
            Resource current = service.getAgent().getStateMachine().getState();
            if (current == null) {
                return new OopsResponse("api.ux.intent.state-unknown", Response.Status.NOT_FOUND).asJSON();
            }
            Collection<Resource> todo = service.getAgent().getStateMachine().getTransitions();
            log.info("ux.intent.before: {} -> {} @ {} ==> {}", action.getAgent(), action.getIntent(), current, todo);

            if (!service.getAgent().getStateMachine().getTransitions().isEmpty()) {
                DataResponse response = new DataResponse(new AgentAction(action.getAgent(), current, todo, (SimpleBindings) my));
                return response.asJSON(Response.Status.MULTIPLE_CHOICES);
            }

            Resource state = service.next(action.getIntent());
            log.info("ux.intent.after: {} == {}", current, state);

//            RDFDump.dump(new LiveModel(connection));
            if (state==null) {
                return new OopsResponse("api.ux.intent.failed", Response.Status.NOT_ACCEPTABLE).asJSON();
            }

            Collection<Resource> intents = service.getAgent().getStateMachine().getTransitions();

            DataResponse response = new DataResponse(new AgentAction(action.getAgent(), state, intents, (SimpleBindings) my));
            return response.asJSON();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new OopsResponse("api.ux.intent.oops", Response.Status.BAD_REQUEST).asJSON();

        }
    }}

