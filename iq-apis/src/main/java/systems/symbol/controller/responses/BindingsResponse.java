package systems.symbol.controller.responses;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.net.URISyntaxException;

public class BindingsResponse implements I_Response {
protected final Logger log = LoggerFactory.getLogger(getClass());
public Bindings bindings = new SimpleBindings();

public BindingsResponse(String iri, GraphQuery query) throws URISyntaxException {
try (GraphQueryResult result = query.evaluate()) {
for (Statement statement : result) {
if (statement.getSubject().stringValue().startsWith(iri) && statement.getObject().isLiteral()) {
// String key = statement.getSubject().stringValue();
String name = statement.getPredicate().stringValue();
if (name.startsWith("urn:") && name.length() > 5) {
bindings.put(name.substring("urn:".length()), statement.getObject().stringValue());
}
}
}
}
}

@Override
public Response build() {
Response.ResponseBuilder build = Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
.entity(bindings);
return addCORS(build).build();
}
}
