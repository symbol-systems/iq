package systems.symbol.controller.responses.ld;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class RdfJsonLdAdapter {

public JsonObject convertRdfToJsonLd(GraphQuery query) throws Exception {
// Collect RDF statements from the query
List<org.eclipse.rdf4j.model.Statement> statements = new ArrayList<>();
StatementCollector collector = new StatementCollector(statements);
query.evaluate(collector);

// Serialize statements to JSON-LD format
// Caused by: jakarta.json.stream.JsonParsingException: JsonParser#getObject() or JsonParser#getObjectStream() is valid only for START_OBJECT parser state. But current parser state is START_ARRAY
try (StringWriter rdfWriter = new StringWriter()) {
Rio.write(statements, rdfWriter, RDFFormat.TURTLE);

// Convert the RDF JSON-LD data to a JsonObject
try (JsonReader jsonReader = Json.createReader(new StringReader(rdfWriter.toString()))) {
return jsonReader.readObject();
}
}
}
}
