package systems.symbol.controller.responses;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

import org.eclipse.rdf4j.query.GraphQuery;
import systems.symbol.controller.responses.ld.RdfJsonLdAdapter;

import java.io.StringWriter;

public class LDResponse implements I_Response {
    private StringWriter $rdf;

    private final RdfJsonLdAdapter rdfJsonLdAdapter = new RdfJsonLdAdapter();

    public LDResponse(GraphQuery query) throws Exception {
        convertToLD(query);
    }

    public void convertToLD(GraphQuery query) throws Exception {
        JsonObject jsonLd = rdfJsonLdAdapter.convertRdfToJsonLd(query);
        try (StringWriter jsonWriter = new StringWriter();
             jakarta.json.JsonWriter writer = Json.createWriter(jsonWriter)) {
            writer.writeObject(jsonLd);
            $rdf = new StringWriter();
            $rdf.write(jsonWriter.toString());
        }
    }

    public Response asJSON() {
        Response.ResponseBuilder builder;
        if ($rdf != null) {
            builder = Response.ok($rdf.toString(), "application/ld+json");
            builder = addCORS(builder);
        } else {
            builder = Response.status(Response.Status.NO_CONTENT);
        }

        // Build and return the response
        return builder.build();
    }

}
