package systems.symbol.controller.llm;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.I_AgentContext;
import systems.symbol.agent.tools.APIException;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.ChatResponse;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.fleet.ExecutiveFleet;
import systems.symbol.llm.Conversation;
import systems.symbol.llm.I_LLM;
import systems.symbol.llm.gpt.GenericGPT;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;

@Path("llm")
public class Agent extends GuardedAPI {

@Path("agent")
@POST
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public Response chat(@HeaderParam("Authorization") String bearer, Conversation chat) throws Exception, APIException {
DecodedJWT jwt = authenticate(bearer);
if (jwt==null) {
return new OopsResponse("api.agent#token-invalid", Response.Status.FORBIDDEN).asJSON();
}
I_Secrets secrets = platform.getSecrets();
String llmToken = secrets.getSecret("OPENAI_API_KEY");
if (Validate.isMissing(llmToken)) {
return new OopsResponse("api.agent#disabled", Response.Status.BAD_REQUEST).asJSON();
}
log.info("api.agent.chat: {}", chat);
Repository repository = platform.getRepository("default");
try (RepositoryConnection connection = repository.getConnection()) {
I_LLM<String> llm = new GenericGPT(llmToken, 1000);
IRI self = Values.iri(jwt.getSubject());
log.info("api.agent.self: {}", self);
Model model = new LiveModel(connection);
//ExecutiveAgent agent = new ExecutiveAgent(self, model);
ExecutiveFleet fleet = new ExecutiveFleet(self, model, secrets, llm);
fleet.deploy();
fleet.start();
I_AgentContext<String, Resource> context = fleet.getContext(self);

context.getConversation().add( chat.latest() );

fleet.run();
fleet.stop();
log.info("api.agent: {}", chat.messages());
return new ChatResponse(chat).asJSON();
}
}
}
