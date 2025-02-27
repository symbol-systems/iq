package systems.symbol.controller.ux;

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
import systems.symbol.controller.platform.RealmAPI;
import systems.symbol.controller.responses.LDResponse;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.rdf4j.util.RDFPrefixer;
import systems.symbol.realm.I_Realm;
import systems.symbol.string.Validate;

import javax.script.Bindings;

@Tag(name = "api.ux.debug.name", description = "api.ux.debug.description")
@Path("ux/debug")
public class DebugAPI extends RealmAPI {

    @GET
    @Operation(summary = "api.ux.debug.post.summary", description = "api.ux.debug.post.description")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/ld+json")
    @Path("{realm}/{self: .*}")
    public Response debug(@PathParam("realm") String _realm, @PathParam("self") String _self,
            @HeaderParam("Authorization") String auth) throws Exception {
        log.info("ux.debug: {} -> {}", _realm, _self);
        if (!Validate.isURN(_self))
            return new OopsResponse("ux.debug.invalid", Response.Status.BAD_REQUEST).build();
        IRI self = Values.iri(_self);
        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("ux.data.realm", Response.Status.NOT_FOUND).build();
        log.debug("ux.debug.realm: {} -> {}", realm.getSelf(), realm.getSecrets());
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }
        if (Validate.isNonAlphanumeric(_realm))
            return new OopsResponse("ux.debug.repository", Response.Status.BAD_REQUEST).build();
        if (!entitled(jwt, _realm))
            return new OopsResponse("ux.debug.trust", Response.Status.FORBIDDEN).build();

        Repository repository = realm.getRepository();
        try (RepositoryConnection connection = repository.getConnection()) {
            LDResponse ld = new LDResponse(RDFPrefixer.describe(connection, self));
            Bindings my = ld.getBindings();

            AgentBuilder builder = new AgentBuilder(self, connection, my, realm.getSecrets());
            I_Agent agent = builder.agent();
            builder.scripting(agent).remodel().sparql(connection);
            agent.start();

            my.put("name", jwt.getClaim("name").asString());
            my.put("audience", jwt.getClaim("aud").asArray(String.class));
            my.put("roles", jwt.getClaim("roles").asArray(String.class));

            log.info("ux.debug.self: {} -> {}", self, my);
            return new SimpleResponse(my).build();
        }
    }

    public boolean entitled(DecodedJWT jwt, String agent) {
        log.info("ux.debug.entitled: {} -> {}", jwt.getAudience(), agent);
        return jwt.getAudience().contains(agent);
    }

    @Override
    public boolean entitled(DecodedJWT jwt, IRI agent) {
        return true;
    }
}
