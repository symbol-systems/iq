package systems.symbol.jgraph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ModelGraph implements Graph<IRI, IRI> {

    private final Model model;
    private IRI defaultPredicate = RDF.TYPE;

    public ModelGraph(Model model) {
        this.model = model;
    }

    @Override
    public Set<IRI> getAllEdges(IRI source, IRI target) {
        return getAllEdges(source, null, target);
    }

    public Set<IRI> getAllEdges(IRI source, IRI predicate, IRI target) {
        Set<IRI> edges = new HashSet<>();
        Iterable<Statement> statements = model.getStatements(source, predicate, target);
        statements.forEach(statement -> edges.add(statement.getPredicate()));
        return edges;
    }

    @Override
    public IRI getEdge(IRI source, IRI target) {
        Iterable<Statement> statements = model.getStatements(source, defaultPredicate, target);
        return null;        
    }

    @Override
    public Supplier<IRI> getVertexSupplier() {
        return null;
    }

    @Override
    public Supplier<IRI> getEdgeSupplier() {
        return null;
    }


    @Override
    public IRI addEdge(IRI source, IRI target) {
        model.add(source, defaultPredicate, target);
        return defaultPredicate;
    }

    @Override
    public boolean addEdge(IRI source, IRI target, IRI defaultEdge) {
        return model.add(source, defaultEdge, target);
    }

    @Override
    public IRI addVertex() {
        return null;
    }

    @Override
    public boolean addVertex(IRI iri) {
        return false;
    }


    @Override
    public boolean containsEdge(IRI source, IRI target) {
        // Implement based on your use case
        return false;
    }

    @Override
    public boolean containsEdge(IRI defaultEdge) {
        // Implement based on your use case
        return false;
    }

    @Override
    public boolean containsVertex(IRI iri) {
        return model.contains(iri, null, null) || model.contains(null, null, iri);
    }

    @Override
    public Set<IRI> edgeSet() {
        Set<IRI> edges = new HashSet<>();
        return edges;
    }

    @Override
    public int degreeOf(IRI vertex) {
        return edgesOf(vertex).size();
    }

    @Override
    public Set<IRI> edgesOf(IRI vertex) {
        Set<IRI> edges = new HashSet<>();
        Iterable<Statement> statements = model.getStatements(vertex, null, null);
        statements.forEach(statement -> edges.add(statement.getPredicate()));
        Iterable<Statement> statements2 = model.getStatements(null, null, vertex);
        statements2.forEach(statement -> edges.add(statement.getPredicate()));
        return edges;
    }

    @Override
    public int inDegreeOf(IRI vertex) {
        Iterable<Statement> inStatements = model.getStatements(null, null, vertex);
        int inDegree = 0;

        for (Statement statement : inStatements) {
            if (statement.getSubject().equals(vertex)) {
                inDegree++;
            }
        }

        return inDegree;
    }

    @Override
    public Set<IRI> incomingEdgesOf(IRI vertex) {
        // Implement based on your use case
        return null;
    }

    @Override
    public int outDegreeOf(IRI vertex) {
        // Implement based on your use case
        return 0;
    }

    @Override
    public Set<IRI> outgoingEdgesOf(IRI vertex) {
        // Implement based on your use case
        return null;
    }

    @Override
    public boolean removeAllEdges(Collection<? extends IRI> edges) {
        // Implement based on your use case
        return false;
    }

    @Override
    public Set<IRI> removeAllEdges(IRI source, IRI target) {
        // Implement based on your use case
        return null;
    }

    @Override
    public boolean removeAllVertices(Collection<? extends IRI> vertices) {
        // Implement based on your use case
        return false;
    }

    @Override
    public IRI removeEdge(IRI source, IRI target) {
        // Implement based on your use case
        return null;
    }

    @Override
    public boolean removeEdge(IRI defaultEdge) {
        // Implement based on your use case
        return false;
    }

    @Override
    public boolean removeVertex(IRI iri) {
        return false;
    }


    @Override
    public Set<IRI> vertexSet() {
        // Implement based on your use case
        return null;
    }

    @Override
    public IRI getEdgeSource(IRI defaultEdge) {
        // Implement based on your use case
        return null;
    }

    @Override
    public IRI getEdgeTarget(IRI iri) {
        return null;
    }

    @Override
    public GraphType getType() {
        return null;
    }

    @Override
    public double getEdgeWeight(IRI iri) {
        return 0;
    }

    @Override
    public void setEdgeWeight(IRI iri, double v) {

    }

}