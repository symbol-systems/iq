package systems.symbol.gql;

import graphql.schema.DataFetchingEnvironment;
import systems.symbol.gql.AskPolicyEngine;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

public class AskPolicyEngineTest {
static Repository repo;
static final String TYPE = "http://example.org/TypeX";
static final String ACTOR = "http://example.org/actor1";

@BeforeAll
public static void setup() throws Exception {
repo = new SailRepository(new MemoryStore());
repo.init();
}

private static Repository repo() {
if (repo == null) {
try {
setup();
} catch (Exception e) {
throw new RuntimeException(e);
}
}
return repo;
}

@AfterAll
public static void teardown() throws Exception {
if (repo!=null) repo.shutDown();
}

DataFetchingEnvironment envWithArg(String key, String val) {
return graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
.arguments(java.util.Map.of(key, val))
.build();
}

@Test
public void testTemplateSubstitutionWithArg() throws Exception {
try (var conn = repo().getConnection()) {
String ttl = "<"+ACTOR+"> <http://example.org/role> \"admin\" .";
conn.add(new StringReader(ttl), "http://example/base", RDFFormat.TURTLE);
}

// template uses arg.role to compare a ***REMOVED*** value
String template = "ASK WHERE { <{actor}> <http://example.org/role> \"{arg.role}\" }";
AskPolicyEngine engine = new AskPolicyEngine(repo(), template, false);

DataFetchingEnvironment env = envWithArg("role", "admin");
boolean allowed = engine.isAllowed(ACTOR, TYPE, env);
assertTrue(allowed);
}

@Test
public void testFallbackAllows() throws Exception {
// primary will check something that doesn't exist
String primaryTemplate = "ASK WHERE { <{actor}> <http://example.org/nope> <{type}> }";
AskPolicyEngine primary = new AskPolicyEngine(repo(), primaryTemplate, false);

// fallback will allow based on canQuery triple
String fallbackTtl = "<"+ACTOR+"> <http://symbol.systems/v0/onto/trust#canQuery> <"+TYPE+"> .";
try (var conn = repo().getConnection()) {
conn.add(new StringReader(fallbackTtl), "http://example/base", RDFFormat.TURTLE);
}
AskPolicyEngine fallback = new AskPolicyEngine(repo(), null, false);

primary.addFallback(fallback);

boolean allowed = primary.isAllowed(ACTOR, TYPE, graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build());
assertTrue(allowed);
}
}
