package systems.symbol.finder;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.testng.annotations.Test;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.io.BootstrapLake;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.store.IQ;
import systems.symbol.rdf4j.store.IQConnection;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FactFinderTest {
File ASSETS_HOME = new File("src/test/resources/assets/");
@Test
public void testHydrateTriple() throws IOException {
BootstrapRepository repo = new BootstrapRepository();
try (RepositoryConnection connection = repo.getConnection()) {
IQ iq = new IQConnection(IQ_NS.TEST, connection);
IQScriptCatalog library = new IQScriptCatalog(iq);

BootstrapLake loader = new BootstrapLake(IQ_NS.TEST, connection, true, true, true, false);
loader.deploy(ASSETS_HOME);


String sparql_index = library.getSPARQL("queries/indexing");
System.out.println("find.sparql_index: "+ sparql_index);
assert sparql_index != null && !sparql_index.isEmpty();

String  sparql_hydrate = library.getSPARQL("queries/hydrating");
System.out.println("find.sparql_hydrate: "+ sparql_hydrate);
assert sparql_hydrate != null && !sparql_hydrate.isEmpty();

TextFinder finder = new TextFinder();
TupleQuery tupleQuery = connection.prepareTupleQuery(sparql_index);
long indexed = IndexHelper.index(finder, tupleQuery);
System.out.println("find.indexed: "+ indexed);
assert indexed > 2;

List<EmbeddingMatch<TextSegment>> found = finder.find("hello");
assert !found.isEmpty();
for(EmbeddingMatch<TextSegment> match : found) {
System.out.println("fact.matched: " + match.embeddingId()+" --" + match.embedded()+" = " + match.score());
}
}
}

}
