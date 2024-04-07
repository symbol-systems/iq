package systems.symbol.gql;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GQLTest {
private final Logger log = LoggerFactory.getLogger(getClass());
String TestSuite = "https://test.symbol.systems/cases#TestSuite";
String inSchemeSPARQL = "SELECT DISTINCT ?_id ?label WHERE {\n" +
"?_id rdf:type skos:Concept.\n" +
"?_id skos:prefLabel ?label.\n" +
"?_id skos:inScheme ?this.\n" +
"}\n";

@Test
public void testLoadSchema() throws IOException {
InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("gql/schema.graphqls");
assert inputStream != null;
String querySchema = IOUtil.readString(inputStream);
assert querySchema != null;
}

@Test
public void testQuery() throws IOException {
InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("gql/schema.graphqls");
assert inputStream!=null;
String querySchema = IOUtil.readString(inputStream);
assert querySchema!=null;

LocalAssetRepository repository = new LocalAssetRepository();
File resourceFolder = new File("src/test/resources/");
IRI ctx = repository.load(resourceFolder, COMMONS.GG_TEST);
assert ctx!=null;
log.info("repository.loaded: " + ctx);

Map map = new HashMap();
map.put("this", TestSuite);

//RepositoryConnection connection = repository.getConnection();
//String prefixClauses = SPARQLQueries.getPrefixClauses(connection.getNamespaces());
//String sparql = "SELECT DISTINCT ?_id ?label ?this WHERE { ?_id rdf:type skos:Concept. ?_id skos:prefLabel ?label. ?_id skos:inScheme ?this.}";
//log.info("pre-test.sparql: " + sparql);
//TupleQuery tupleQuery = connection.prepareTupleQuery(prefixClauses+"SELECT DISTINCT ?_id ?label ?this WHERE { ?_id rdf:type skos:Concept. ?_id skos:prefLabel ?label. ?_id skos:inScheme ?this.}");
//Queries.setBindings(connection.getValueFactory(), tupleQuery, map);
//Collection<Map<String, Object>> models = Queries.models(tupleQuery.evaluate());
//assert models!=null;
//log.info("pre-test.found: " + models.size());
//assert models.size()==4;
//connection.close();
//
GQL gql = new GQL();
TypeDefinitionRegistry parsed = gql.parse(querySchema);
assert parsed != null;
GraphQL graphQL = gql.newGraphQL(repository);
assert graphQL != null;

String query = "query inScheme($this: ID!) { concepts: skos_inScheme(this: $this) { _id, label } }";
ExecutionResult executionResult = gql.execute(repository, graphQL, query, map);
log.info("GQL.getErrors: " + executionResult.getErrors());
assert executionResult != null;
assert executionResult.getErrors()!=null;
assert executionResult.getErrors().isEmpty();
log.info("GQL.getData: " + executionResult.getData());
assert executionResult.getData() != null;
Map data = ((Map)executionResult.getData());
assert data!=null;
assert data.containsKey("concepts");
log.info("GQL.concepts.found: " + data.get("concepts"));
assert ((Collection)data.get("concepts")).size()==4;
}
}
