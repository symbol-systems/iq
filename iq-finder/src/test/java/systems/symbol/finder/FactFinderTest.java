package systems.symbol.finder;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.io.BootstrapLake;
import systems.symbol.rdf4j.store.BootstrapRepository;
import systems.symbol.rdf4j.sparql.IQSimpleCatalog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FactFinderTest {
    File ASSETS_HOME = new File("src/test/resources/assets/");

    @Test
    public void testHydrateTriple() throws IOException {
        BootstrapRepository repo = new BootstrapRepository();
        try (RepositoryConnection connection = repo.getConnection()) {
            IQSimpleCatalog library = new IQSimpleCatalog();

            BootstrapLake loader = new BootstrapLake(IQ_NS.TEST, connection, true, true, true, false);
            loader.deploy(ASSETS_HOME);

            String sparql_index = library.getScript("index");
            System.out.println("find.sparql_index: " + sparql_index);
            assert sparql_index != null && !sparql_index.isEmpty();

            // String sparql_hydrate = library.getSPARQL("hydrating");
            // System.out.println("find.sparql_hydrate: " + sparql_hydrate);
            // assert sparql_hydrate != null && !sparql_hydrate.isEmpty();

            TextFinder finder = new TextFinder();
            GraphQuery query = connection.prepareGraphQuery(sparql_index);
            long indexed = IndexHelper.index(finder, query.evaluate());
            System.out.println("find.indexed: " + indexed);
            assert indexed > 1;

            List<EmbeddingMatch<TextSegment>> found = finder.find("IQ");
            assert !found.isEmpty();
            for (EmbeddingMatch<TextSegment> match : found) {
                System.out.println(
                        "fact.matched: " + match.embeddingId() + " --" + match.embedded() + " = " + match.score());
            }
        }
    }

}
