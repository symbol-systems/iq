package coded.claims.camel.processor.rdf4j;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class RDF4JProcessorTest {
static Repository repo;

@BeforeAll
public static void initRepo() throws Exception {
repo = new SailRepository(new MemoryStore());
repo.init();
try (var conn = repo.getConnection()) {
String ttl = "@prefix ex: <http://example/> .\nex:s ex:p \"o\" .";
conn.add(new StringReader(ttl), "http://example/base", RDFFormat.TURTLE);
}
}

@AfterAll
public static void shutdownRepo() throws Exception {
if (repo != null) repo.shutDown();
}

@Test
public void testSelectJsonResults() throws Exception {
var ctx = new DefaultCamelContext();
Exchange ex = new DefaultExchange(ctx);
ex.getIn().setBody("SELECT ?s ?p ?o WHERE { ?s ?p ?o }");
ex.getIn().setHeader("Accept", "application/sparql-results+json");

var processor = new coded.claims.camel.processor.rdf4j.RDF4JProcessor(repo, "select", true, 0, null);
processor.process(ex);

String body = ex.getMessage().getBody(String.class);
assertNotNull(body);
assertTrue(body.contains("results") || body.contains("bindings"));
}

@Test
public void testConstructRdfDefault() throws Exception {
var ctx = new DefaultCamelContext();
Exchange ex = new DefaultExchange(ctx);
ex.getIn().setBody("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
// no Accept header -> default to returning GraphQueryResult object

var processor = new coded.claims.camel.processor.rdf4j.RDF4JProcessor(repo, "construct", true, 0, null);
processor.process(ex);

Object body = ex.getMessage().getBody();
assertNotNull(body);
}

@Test
public void testConstructTurtle() throws Exception {
var ctx = new DefaultCamelContext();
Exchange ex = new DefaultExchange(ctx);
ex.getIn().setBody("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
ex.getIn().setHeader("Accept", "text/turtle");

var processor = new coded.claims.camel.processor.rdf4j.RDF4JProcessor(repo, "construct", true, 0, null);
processor.process(ex);

String body = ex.getMessage().getBody(String.class);
assertNotNull(body);
assertTrue(body.contains("@prefix") || body.contains("<http://"));
}

@Test
public void testConstructJsonLd() throws Exception {
var ctx = new DefaultCamelContext();
Exchange ex = new DefaultExchange(ctx);
ex.getIn().setBody("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }");
ex.getIn().setHeader("Accept", "application/ld+json");

var processor = new coded.claims.camel.processor.rdf4j.RDF4JProcessor(repo, "construct", true, 0, null);
processor.process(ex);

String body = ex.getMessage().getBody(String.class);
assertNotNull(body);
assertTrue(body.contains("@context") || body.trim().startsWith("{"));
}
}
