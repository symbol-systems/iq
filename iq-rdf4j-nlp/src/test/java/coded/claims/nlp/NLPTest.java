package systems.symbol.nlp;

import systems.symbol.jgraph.Graphs;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.sparql.SPARQLMapper;
import systems.symbol.rdf4j.util.UsefulSPARQL;
import systems.symbol.rdf4j.iq.KBMS;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import systems.symbol.rdf4j.store.Workspace;
import systems.symbol.rdf4j.util.RDFHelper;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.jgrapht.Graph;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NLPTest {
Repository repository;

String text_sentence = "Welcome to the Jungle. You're gonna die!";

@BeforeMethod
public void setUp() throws IOException {
repository = new LocalAssetRepository(new File("src/test/resources/assets/triples/"), NS.GG_TEST);
new File("tmp").mkdir();
}

@AfterMethod
public void tearDown() {
repository.shutDown();
}

@Test
public void testReset() {
}

@Test
public void testParseSimple() throws IOException {
NLP nlp = new NLP();
Model tokens = nlp.parse(text_sentence);
assert tokens!=null;
assert tokens.size() >20 ;
System.out.println("nlp.parse.text: "+tokens.size());
RDFHelper.dump( tokens, new File("tmp/nlp.ttl"));
}

@Test
public void testParseBulk() throws IOException {
RepositoryConnection connection = repository.getConnection();
KBMS kbms = new KBMS(NS.GG_TEST, connection);
SPARQLMapper SPARQLMapper = new SPARQLMapper(kbms);
System.out.println("nlp.bulk.count: "+SPARQLMapper.count());

NLP nlp = new NLP();
InputStream textStream = nlp.load("nlp/data-governance.txt");
String textString = IOUtil.readString(textStream);
System.out.println("nlp.bulk.parse: "+SPARQLMapper.count());
Model tokens = nlp.parse(textString);
assert tokens!=null;
assert tokens.size() > 1000;
System.out.println("nlp.bulk.parsed: "+tokens.size());
//File file = RDFHelper.dump( tokens, new File("tmp/nlp-bulk.ttl"));
//System.out.println("nlp.bulk.written: "+file.length());
}

@Test
public void testParseBulkAnalyse() throws IOException {
NLP nlp = new NLP();
InputStream textStream = nlp.load("nlp/data-governance.txt");
String textString = IOUtil.readString(textStream);
Model tokens = nlp.parse(textString);
assert tokens!=null;
assert tokens.size() > 1000;
System.out.println("nlp.bulk.analyse.parse: "+tokens.size());

Graph<Resource, Resource> tokenGraph2 = Graphs.toGraph(tokens);
Graphs.pageRank(tokenGraph2, tokens);
RDFHelper.dump(tokens, new File("tmp/nlp-bulk-analyse.ttl"));

Repository repository = Workspace.create(tokens);
RepositoryConnection connection = repository.getConnection();
String query = "SELECT DISTINCT ?this ?score WHERE { ?s <urn:iq:nlp:nextToken> ?this.?this <urn:iq:graph:rank:score> ?score. } GROUP BY ?this ?score ORDER BY desc(?score)";
//System.out.println("nlp.bulk.analyse.models.query: "+query);

SPARQLMapper SPARQLMapper = new SPARQLMapper(new KBMS(NS.GG_TEST, connection));

TupleQuery tupleQuery = connection.prepareTupleQuery(query);
List<Map<String,Object>> models = SPARQLMapper.queryResultsToModels(tupleQuery.evaluate());
assert models!=null && !models.isEmpty();
System.out.println("nlp.bulk.analyse.models.ranked");

TupleQuery query1 = SPARQLMapper.toTupleQuery(UsefulSPARQL.COUNT, null);
System.out.println("nlp.bulk.analyse.models.query1: "+query1);
List<Map<String,Object>> counted = SPARQLMapper.queryResultsToModels(query1.evaluate());
System.out.println("nlp.bulk.analyse.models.counted: "+counted);
}

@Test
public void testParseTokens() throws IOException {
NLP nlp = new NLP();
Collection tokens = nlp.parseTokens(text_sentence);
assert tokens!=null;
assert tokens.size() == 9 ;
System.out.println("nlp.tokens: "+tokens);
}

@Test
public void testParsePartOfSpeech() throws IOException {
NLP nlp = new NLP();
Map<String, String> pos = nlp.parsePartOfSpeech(text_sentence);
assert pos!=null;
System.out.println("nlp.pos: "+pos);
assert pos.size() == 8 ;
}

@Test
public void testParseSentences() throws IOException {
NLP nlp = new NLP();
String[] sentences = nlp.parseSentences(text_sentence);
assert sentences!=null;
assert sentences.length == 2 ;
System.out.println("nlp.sentences: "+String.join(",", sentences));
}

}