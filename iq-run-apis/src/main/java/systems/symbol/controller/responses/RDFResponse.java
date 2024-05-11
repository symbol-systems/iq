package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.util.RDFPrefixer;

import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Map;

public class RDFResponse implements I_Response {
public static final MediaType JSON_LD = MediaType.valueOf("application/ld+json");
protected final Logger log = LoggerFactory.getLogger(getClass());
StringWriter rdf$ = new StringWriter();

public RDFResponse(String iri, GraphQuery query, RDFFormat format) throws URISyntaxException {
RDFWriter writer = Rio.createWriter(format, this.rdf$, iri);
writer.setWriterConfig(getWriterConfig());
writer.startRDF();
Map<String, String> namespaces = RDFPrefixer.defaults();
for(String ns: namespaces.keySet()) {
writer.handleNamespace(ns, namespaces.get(ns));
}
if (iri!=null&&!iri.isEmpty()) writer.handleNamespace("", iri);
try (GraphQueryResult result = query.evaluate()) {
for (Statement statement : result) {
writer.handleStatement(statement);
//log.info(statement.toString());
}
}
writer.endRDF();
}

public RDFResponse(String iri, Model model, RDFFormat format) throws URISyntaxException {
RDFWriter writer = Rio.createWriter(format, this.rdf$, iri);
writer.setWriterConfig(getWriterConfig());
writer.startRDF();
Map<String, String> namespaces = RDFPrefixer.defaults();
for(String ns: namespaces.keySet()) {
writer.handleNamespace(ns, namespaces.get(ns));
}
if (iri!=null&&!iri.isEmpty()) writer.handleNamespace("", iri);
for (Statement statement : model) {
writer.handleStatement(statement);
}
writer.endRDF();
}

@Override
public Response asJSON() {
Response.ResponseBuilder build = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON).entity(rdf$.toString());
return addCORS(build).build();
}

public Response asJSONLD() {
Response.ResponseBuilder build = Response.status(Response.Status.OK).type(JSON_LD).entity(rdf$.toString());
return addCORS(build).build();
}

public static WriterConfig getWriterConfig() {
WriterConfig config = RDFDump.getWriterConfig();
config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.COMPACT);
config.set(JSONLDSettings.HIERARCHICAL_VIEW, Boolean.TRUE);
config.set(JSONLDSettings.OPTIMIZE, Boolean.TRUE);
return config;
}
}
