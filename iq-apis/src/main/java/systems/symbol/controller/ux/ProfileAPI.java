package systems.symbol.controller.ux;

import java.io.IOException;

import javax.script.SimpleBindings;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import systems.symbol.controller.platform.GuardedAPI;
import systems.symbol.controller.responses.OopsException;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.controller.responses.SimpleResponse;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;
import systems.symbol.string.Validate;

/**
 * RESTful endpoint for returning RDF values from repositories.
 */
@Path("/ux/profile")
@Tag(name = "api.ux.profile.name", description = "api.ux.profile.description")
public class ProfileAPI extends GuardedAPI {
    Gson gson = new Gson();
    IRI JSON_TYPE = Values.iri(RDF.NAMESPACE + "JSON");

    /**
     * Return the value from RDF repository.
     *
     * @param _realm The name of the RDF repository.
     * @param path   The path of a resource.
     * @return Response containing the results of the path as a string.
     */
    @GET
    @Operation(summary = "api.ux.profile.get.summary", description = "api.ux.profile.get.description")
    @Path("{realm}")
    public Response loadProfile(@PathParam("realm") String _realm, @Context UriInfo uriInfo,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        log.info("ux.profile: {} --> {}", _realm, uriInfo.getQueryParameters().keySet());

        if (Validate.isNonAlphanumeric(_realm))
            return new OopsResponse("ux.profile.realm", Response.Status.BAD_REQUEST).build();
        if (!Validate.isBearer(auth))
            return new OopsResponse("ux.profile.unauthorized", Response.Status.UNAUTHORIZED).build();

        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("ux.profile.realm", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }

        log.info("ux.profile.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

        I_Realm my_realm = platform.getRealm(jwt.getSubject());
        if (my_realm == null) {
            return new OopsResponse("ux.profile.realm.user", Response.Status.NOT_FOUND).build();
        }
        Repository repository = my_realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.profile.repository", Response.Status.NOT_FOUND).build();

        try (RepositoryConnection connection = repository.getConnection()) {
            IRI resource = Values.iri(jwt.getSubject());
            RepositoryResult<Statement> found = connection.getStatements(resource, RDF.VALUE, null);
            if (!found.hasNext()) {
                return new SimpleResponse().build();
            }
            Statement stmt = found.next();
            found.close();
            Value object = stmt.getObject();
            if (!object.isLiteral()) {
                return new OopsResponse("ux.profile.invalid", Response.Status.NO_CONTENT).build();
            }
            Literal value = (Literal) object;
            log.info("ux.profile.found: {} --> {}", value.stringValue(), value.getDatatype());
            if (!value.getDatatype().equals(JSON_TYPE)) {
                return new OopsResponse("ux.profile.json", Response.Status.NO_CONTENT).build();
            }
            SimpleBindings json = gson.fromJson(value.stringValue(), SimpleBindings.class);
            return new SimpleResponse(json).build();
        } catch (Exception e) {
            log.error("ux.profile.failed: {} ==> {}", jwt.getSubject(), e.getMessage());
            return new OopsResponse("ux.profile.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save the value to RDF repository.
     *
     * @param _realm The name of the RDF repository.
     * @param path   The path of a resource.
     * @return Response containing the results of the path as a string.
     */
    @POST
    @Operation(summary = "api.ux.profile.post.summary", description = "api.ux.profile.post.description")
    @Path("{realm}")
    public Response saveProfile(@PathParam("realm") String _realm, @Context UriInfo uriInfo,
            @HeaderParam("Authorization") String auth, SimpleBindings json) throws IOException, SecretsException {
        log.info("ux.profile: {} --> {}", _realm, uriInfo.getQueryParameters().keySet());

        if (Validate.isNonAlphanumeric(_realm))
            return new OopsResponse("ux.profile.realm", Response.Status.BAD_REQUEST).build();
        if (!Validate.isBearer(auth))
            return new OopsResponse("ux.profile.unauthorized", Response.Status.UNAUTHORIZED).build();

        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("ux.profile.realm", Response.Status.NOT_FOUND).build();
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }

        log.info("ux.profile.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

        I_Realm my_realm = platform.getRealm(jwt.getSubject());
        Repository repository = my_realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.profile.repository", Response.Status.NOT_FOUND).build();

        try (RepositoryConnection connection = repository.getConnection()) {
            IRI resource = Values.iri(jwt.getSubject());
            connection.remove(resource, RDF.VALUE, null);
            Literal value = Values.literal(gson.toJson(json), JSON_TYPE);
            connection.add(resource, RDF.VALUE, value, resource);
            log.info("ux.profile.saved: {} --> {}", value.stringValue(), value.getDatatype());
            connection.commit();
            return new SimpleResponse(json).build();
        } catch (Exception e) {
            log.error("ux.profile.failed: {} ==> {}", jwt.getSubject(), e.getMessage());
            return new OopsResponse("ux.profile.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
