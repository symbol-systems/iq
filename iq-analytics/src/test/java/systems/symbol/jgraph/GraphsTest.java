package systems.symbol.jgraph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultEdge;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.platform.IQ_NS;
import systems.symbol.rdf4j.store.BootstrapRepository;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GraphsTest {
    private static final Logger log = LoggerFactory.getLogger(GraphsTest.class);

    static BootstrapRepository repository;
    static IRI ctx, alice, bob, charlie, delta, knows;
    // TripleSource triples;

    @BeforeAll
    public static void bootstrap() throws IOException {
        repository = new BootstrapRepository();
        ctx = repository.load(new File("src/test/resources/"), IQ_NS.TEST);
        log.info("iq.graph.loaded: {}", ctx);
        assert ctx != null;
        alice = repository.getValueFactory().createIRI("iq:test:Alice");
        bob = repository.getValueFactory().createIRI("iq:test:Bob");
        charlie = repository.getValueFactory().createIRI("iq:test:Charlie");
        delta = repository.getValueFactory().createIRI("iq:test:Delta");
        knows = repository.getValueFactory().createIRI("iq:test:knows");

        // triples = repository.getTripleSource();
    }

    @Test
    public void testFromRDF4J() throws IOException {
        if (repository == null) {
            log.error("iq.graph.rdf.repository.missing");
            return;
        }
        Graph<Resource, DefaultEdge> src_graph = Graphs.toGraph();

        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, DefaultEdge> graph = Graphs.toGraph(src_graph, statements.iterator());
            log.info("iq.graph.rdf:" + graph);
            assert src_graph == graph;
            assert graph.containsVertex(alice);
            assert graph.containsVertex(bob);
            assert graph.containsVertex(charlie);
            assert graph.containsEdge(alice, bob);
        }
    }

    @Test
    public void testFindDirectedPaths() {
        if (repository == null) {
            log.error("iq.graph.paths.repository.missing");
            return;
        }
        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, DefaultEdge> graph = Graphs.toGraph(statements.iterator());
            Collection<GraphPath<Resource, DefaultEdge>> paths = Graphs.findDirectedPaths(graph, alice, charlie);
            log.info("iq.graph.paths.found: " + paths.size() + " ->" + paths);
            assert paths != null;
            assert paths.size() > 0;
        }

    }

    @Test
    public void testPredictLink() {
        if (repository == null) {
            log.error("iq.graph.predict.repository.missing");
            return;
        }
        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, DefaultEdge> graph = Graphs.toGraph(statements.iterator());
            // log.info("iq.graph.predict.graph: " + graph);
            assert graph.containsVertex(alice);
            double confidence1 = Graphs.predictLinkCommonNeighbors(graph, alice, bob);
            log.info("iq.graph.predict.link.1: " + confidence1);
            // assert confidence1==1.0;

            double confidence2 = Graphs.predictLinkHubPromoted(graph, bob, charlie);
            log.info("iq.graph.predict.link.2: " + confidence2);
            // assert confidence2==1.0;
        }
    }

    @Test
    public void testPredictKatzCentrality() {
        if (repository == null) {
            log.error("iq.graph.katz.repository.missing");
            return;
        }
        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, DefaultEdge> graph = Graphs.toGraph(statements.iterator());
            Map<Resource, Double> found = Graphs.findKatzCentrality(graph);
            log.info("iq.graph.katz.scores: " + found);
            assert found != null;
            assert !found.isEmpty();
            assert 1.0 == found.get(alice);
        }
    }
}