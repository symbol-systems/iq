package systems.symbol.finder;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.BulkAssetLoader;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.rdf4j.iq.IQConnection;
import systems.symbol.rdf4j.sparql.ScriptCatalog;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FactFinderTest {
    File ASSETS_HOME = new File("src/test/resources/assets/");
    @Test
    public void testHydrateTriple() throws IOException {
        LocalAssetRepository repo = new LocalAssetRepository();
        try (RepositoryConnection connection = repo.getConnection()) {
            IQ iq = new IQConnection(COMMONS.IQ_NS_TEST, connection);
            ScriptCatalog library = new ScriptCatalog(iq);

            BulkAssetLoader loader = new BulkAssetLoader(COMMONS.IQ_NS_TEST, connection, true, true, true, false);
            loader.deploy(ASSETS_HOME);


            String sparql_index = library.getSPARQL("queries/indexing.sparql");
            System.out.println("find.sparql_index: "+ sparql_index);
            assert sparql_index != null && !sparql_index.isEmpty();

            String  sparql_hydrate = library.getSPARQL("queries/hydrating.sparql");
            System.out.println("find.sparql_hydrate: "+ sparql_hydrate);
            assert sparql_hydrate != null && !sparql_hydrate.isEmpty();

            TextFinder finder = new TextFinder();
            TupleQuery tupleQuery = connection.prepareTupleQuery(sparql_index);
            long indexed = IndexHelper.index(finder, tupleQuery, "this", "text");
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
