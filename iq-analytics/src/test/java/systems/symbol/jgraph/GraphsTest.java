package systems.symbol.jgraph;

import systems.symbol.COMMONS;
import systems.symbol.rdf4j.store.BootstrapRepository;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class GraphsTest {
    private static final Logger log = LoggerFactory.getLogger( GraphsTest.class );

    BootstrapRepository repository;
    IRI ctx, alice, bob, charlie, delta, knows;
//    TripleSource triples;

    @BeforeTest
    void bootstrap() throws IOException {
        repository = new BootstrapRepository();
        ctx = repository.load(new File("src/test/resources/"), IQ_NS.TEST);
        assert ctx!=null;
        alice = repository.getValueFactory().createIRI("urn:iq:test:Alice");
        bob = repository.getValueFactory().createIRI("urn:iq:test:Bob");
        charlie = repository.getValueFactory().createIRI("urn:iq:test:Charlie");
        delta = repository.getValueFactory().createIRI("urn:iq:test:Delta");
        knows = repository.getValueFactory().createIRI("urn:iq:test:knows");

//        triples = repository.getTripleSource();
    }

    @Test
    public void testFromRDF4J() throws IOException {
        Graph<Resource, Resource> src_graph = new DefaultDirectedWeightedGraph(DefaultWeightedEdge.class);

        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, Resource> graph = Graphs.toGraph(src_graph, statements.stream());
            log.info("iq.graph.rdf:" +graph);
            assert src_graph == graph;
            assert graph.containsVertex(alice);
            assert graph.containsVertex(bob);
            assert graph.containsVertex(charlie);
            assert graph.containsEdge(alice, bob);
        }
    }

    @Test
    public void testFromRDF() {
        // TODO
    }

    @Test
    public void testFindDirectedPaths() {
        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, Resource> graph = Graphs.toGraph(statements.stream());
            Collection<GraphPath> paths = Graphs.findDirectedPaths(graph, alice, charlie);
            log.info("iq.graph.found.paths: " + paths.size() + " ->" + paths);
            assert paths!=null;
            assert paths.size()>0;
        }

    }

    @Test
    public void testPredictLink() {
        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, Resource> graph = Graphs.toGraph(statements.stream());
//        log.info("iq.graph.predict.graph: " + graph);
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
        try (RepositoryConnection connection = repository.getConnection()) {
            RepositoryResult<Statement> statements = connection.getStatements(null, knows, null, ctx);
            Graph<Resource, Resource> graph = Graphs.toGraph(statements.stream());
            Map<Object, Double> found = Graphs.findKatzCentrality(graph);
            log.info("iq.graph.katz.scores: " + found);
            assert found!=null;
            assert !found.isEmpty();
            assert 1.0 == found.get(alice);
        }
    }
}