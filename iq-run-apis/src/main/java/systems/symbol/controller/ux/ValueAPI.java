package systems.symbol.controller.ux;

import java.io.IOException;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
@Path("/ux/value")
@Tag(name = "api.ux.value.name", description = "api.ux.value.description")
public class ValueAPI extends GuardedAPI {

    /**
     * Return the value from RDF repository.
     *
     * @param _realm The name of the RDF repository.
     * @param path   The path of a resource.
     * @return Response containing the results of the path as a string.
     */
    @GET
    @Operation(summary = "api.ux.value.get.summary", description = "api.ux.value.get.description")
    @Path("{realm}/{path: .*}")
    public Response value(@PathParam("realm") String _realm,
            @PathParam("path") String path,
            @Context UriInfo uriInfo,
            @HeaderParam("Authorization") String auth) throws IOException, SecretsException {
        log.info("ux.value: {} --> {} -> {}", _realm, path, uriInfo.getQueryParameters().keySet());

        if (path.isEmpty())
            return new OopsResponse("ux.value.path", Response.Status.BAD_REQUEST).build();
        if (Validate.isNonAlphanumeric(_realm))
            return new OopsResponse("ux.value.realm", Response.Status.BAD_REQUEST).build();
        if (!Validate.isBearer(auth))
            return new OopsResponse("ux.value.unauthorized", Response.Status.UNAUTHORIZED).build();

        I_Realm realm = platform.getRealm(_realm);
        if (realm == null)
            return new OopsResponse("ux.value.realm", Response.Status.NOT_FOUND).build();
        if (!Validate.isURN(path)) {
            path = realm.getSelf() + path;
        }
        DecodedJWT jwt;
        try {
            jwt = authenticate(auth, realm);
        } catch (OopsException e) {
            return new OopsResponse(e.getMessage(), e.getStatus()).build();
        }

        log.info("ux.value.jwt: {} --> {} -> {}", jwt.getSubject(), jwt.getAudience(), jwt.getIssuer());

        Repository repository = realm.getRepository();
        if (repository == null)
            return new OopsResponse("ux.value.repository", Response.Status.NOT_FOUND).build();

        try (RepositoryConnection connection = repository.getConnection()) {
            IRI resource = Values.iri(path);
            RepositoryResult<Statement> found = connection.getStatements(resource, RDF.VALUE, null);
            if (!found.hasNext()) {
                return new OopsResponse("ux.value.missing", Response.Status.NOT_FOUND).build();
            }
            found.hasNext();
            Statement stmt = found.next();
            found.close();
            Value object = stmt.getObject();
            if (!object.isLiteral()) {
                return new OopsResponse("ux.value.invalid", Response.Status.NOT_FOUND).build();
            }
            Literal value = (Literal) object;
            log.info("ux.value.found: {} --> {}", value.stringValue(), value.getDatatype());
            return new SimpleResponse(value.stringValue()).type("text/plain").build();
        } catch (Exception e) {
            log.error("ux.value.failed: {} -> {} ==> {}", jwt.getSubject(), path, e.getMessage());
            return new OopsResponse("ux.value.failed", Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
