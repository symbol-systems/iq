package systems.symbol.gql;

import systems.symbol.gql.AskPolicyEngine;
import systems.symbol.gql.AuthorizationDataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.collections4.map.HashedMap;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationDataFetcherTest {
static Repository repo;
static final String TYPE = "http://example.org/TypeX";
static final String ACTOR = "http://example.org/actor1";

@BeforeAll
static void setup() throws Exception {
repo = new SailRepository(new MemoryStore());
repo.init();
try (var conn = repo.getConnection()) {
// add an example ACL triple for the authorized test when needed within tests
}
}

@AfterAll
static void teardown() throws Exception {
if (repo!=null) repo.shutDown();
}

DataFetchingEnvironment buildEnvWithActor(String actor) {
// Use a minimal DataFetchingEnvironment via builder
return graphql.schema.DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
.arguments(java.util.Map.of("actor", actor))
.build();
}

@Test
void testDeniedWhenNoAcl() throws Exception {
graphql.schema.DataFetcher<java.util.Collection<java.util.Map<String,Object>>> delegate = (DataFetchingEnvironment env) -> Collections.emptyList();
// use AskPolicyEngine
AskPolicyEngine peg = new AskPolicyEngine(repo, null);
AuthorizationDataFetcher auth = new AuthorizationDataFetcher(TYPE, peg, delegate);

DataFetchingEnvironment env = buildEnvWithActor(ACTOR);
assertThrows(SecurityException.class, () -> auth.get(env));
}

@Test
void testAllowedWhenAclPresent() throws Exception {
try (var conn = repo.getConnection()) {
// Add ASK triple: <actor> <acl#canQuery> <TYPE>
String ttl = "@prefix acl: <http://symbol.systems/v0/onto/trust#> .\n" +
"<"+ACTOR+"> acl:canQuery <"+TYPE+"> .";
conn.add(new StringReader(ttl), "http://example/base", RDFFormat.TURTLE);
}

graphql.schema.DataFetcher<java.util.Collection<java.util.Map<String,Object>>> delegate = (DataFetchingEnvironment env) -> List.of(Map.of("ok", true));
AskPolicyEngine peg = new AskPolicyEngine(repo, null);
AuthorizationDataFetcher auth = new AuthorizationDataFetcher(TYPE, peg, delegate);

DataFetchingEnvironment env = buildEnvWithActor(ACTOR);
Collection<Map<String,Object>> res = auth.get(env);
assertNotNull(res);
assertEquals(1, res.size());
}
}
