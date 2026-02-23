package systems.symbol.gql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import systems.symbol.gql.GQL;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GQLAuthEndToEndTest {
static Repository repo;
static final String ACTOR = "http://example.org/actor1";
static final String TYPE = "http://example.org/TypeX";

@BeforeAll
public static void setup() throws Exception {
repo = new SailRepository(new MemoryStore());
repo.init();

// add a sample entity triple
try (var conn = repo.getConnection()) {
String ttl = "@prefix ex: <http://example.org/> .\n" +
"ex:thing1 a <"+TYPE+"> ; ex:name \"Thing One\" .";
conn.add(new StringReader(ttl), "http://example/base", RDFFormat.TURTLE);
}
// add a policy and a supporting altCanQuery triple used by the policy template
try (var conn = repo.getConnection()) {
String policy = "<http://example.org/policy1> <http://symbol.systems/v0/onto/trust#forType> <"+TYPE+"> .\n" +
"<http://example.org/policy1> <http://symbol.systems/v0/onto/trust#askTemplate> \"ASK WHERE { <{actor}> <http://example.org/altCanQuery> <{type}> }\" .\n" +
"<"+ACTOR+"> <http://example.org/altCanQuery> <"+TYPE+"> .";
conn.add(new StringReader(policy), "http://example/policies", RDFFormat.TURTLE);
}
}

@AfterAll
public static void tearDown() throws Exception {
if (repo!=null) repo.shutDown();
}

@Test
public void testGraphqlAuthorized() throws Exception {
// minimal schema with rdf directive on type
String schema = "directive @rdf(iri: String) on OBJECT | FIELD_DEFINITION\n" +
"type Thing @rdf(iri: \"http://example.org/TypeX\") { id: ID name: String }\n" +
"type Query { things(actor: String): [Thing] }";

GQL gqlT = new GQL();
var execSchema = gqlT.makeExecutableSchema(schema, repo);
GraphQL graphQL = GraphQL.newGraphQL(execSchema).build();

String q = "{ things(actor:\""+ACTOR+"\") { id name } }";
ExecutionInput input = ExecutionInput.newExecutionInput().query(q).build();
ExecutionResult res = graphQL.execute(input);
assertTrue(res.getErrors().isEmpty(), () -> "Errors: " + res.getErrors());
Map<String,Object> data = res.getData();
assertNotNull(data);
assertTrue(data.get("things")!=null);
}

@Test
public void testGraphqlDenied() throws Exception {
String schema = "directive @rdf(iri: String) on OBJECT | FIELD_DEFINITION\n" +
"type Thing @rdf(iri: \"http://example.org/TypeX\") { id: ID name: String }\n" +
"type Query { things(actor: String): [Thing] }";

GQL gqlT = new GQL();
var execSchema = gqlT.makeExecutableSchema(schema, repo);
GraphQL graphQL = GraphQL.newGraphQL(execSchema).build();

String q = "{ things(actor:\"http://example.org/bad\") { id name } }";
ExecutionInput input = ExecutionInput.newExecutionInput().query(q).build();
ExecutionResult res = graphQL.execute(input);
assertFalse(res.getErrors().isEmpty());
}
}
