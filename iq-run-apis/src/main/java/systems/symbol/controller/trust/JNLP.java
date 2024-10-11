package systems.symbol.controller.trust;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.controller.responses.OopsResponse;
import systems.symbol.rdf4j.io.IOCopier;
import systems.symbol.render.HBSRenderer;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.InputStream;

@Tag(name = "api.trust.jnlp.app", description = "api.trust.jnlp.description")
@Path("/jnlp")
public class JNLP {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders headers;

    @GET
    @Path("/{app}")
    @Produces(MediaType.APPLICATION_XML)
    public Response launcher(@PathParam("app") String app) throws IOException {
        String host = uriInfo.getBaseUri().toString();
        String codebase = host + "jnlp";
        InputStream jnlpStream = getClass().getResourceAsStream("/" + app + ".jnlp");
        log.info("jnlp.app: {} => {} == {}", host, app, jnlpStream != null);
        if (jnlpStream == null) {
            return new OopsResponse("api.trust.jnlp.app", Response.Status.NOT_FOUND).build();
        }

        Bindings ctx = new SimpleBindings();
        ctx.put("host", host);
        ctx.put("app", app);
        ctx.put("codebase", codebase);

        String template = HBSRenderer.template(IOCopier.toString(jnlpStream), ctx);
        return Response.ok(template).build();
    }
}
