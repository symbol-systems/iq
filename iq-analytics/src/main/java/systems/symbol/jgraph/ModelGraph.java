package systems.symbol.jgraph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.graph.DefaultGraphType;

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
        for (Statement statement : statements) {
            return statement.getPredicate();
        }
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
    public boolean addEdge(IRI source, IRI target, IRI predicate) {
        model.add(source, predicate, target);
        return true;
    }

    @Override
    public IRI addVertex() {
        return null; // This depends on how you want to generate new vertices
    }

    @Override
    public boolean addVertex(IRI vertex) {
        return !containsVertex(vertex);
    }

    @Override
    public boolean containsEdge(IRI source, IRI target) {
        return model.contains(source, null, target);
    }

    @Override
    public boolean containsEdge(IRI edge) {
        return model.contains(null, edge, null);
    }

    @Override
    public boolean containsVertex(IRI vertex) {
        return model.contains(vertex, null, null) || model.contains(null, null, vertex);
    }

    @Override
    public Set<IRI> edgeSet() {
        Set<IRI> edges = new HashSet<>();
        model.forEach(statement -> edges.add(statement.getPredicate()));
        return edges;
    }

    @Override
    public int degreeOf(IRI vertex) {
        return edgesOf(vertex).size();
    }

    @Override
    public Set<IRI> edgesOf(IRI vertex) {
        Set<IRI> edges = new HashSet<>();
        model.getStatements(vertex, null, null).forEach(statement -> edges.add(statement.getPredicate()));
        model.getStatements(null, null, vertex).forEach(statement -> edges.add(statement.getPredicate()));
        return edges;
    }

    @Override
    public int inDegreeOf(IRI vertex) {
        return incomingEdgesOf(vertex).size();
    }

    @Override
    public Set<IRI> incomingEdgesOf(IRI vertex) {
        Set<IRI> incomingEdges = new HashSet<>();
        model.getStatements(null, null, vertex).forEach(statement -> incomingEdges.add(statement.getPredicate()));
        return incomingEdges;
    }

    @Override
    public int outDegreeOf(IRI vertex) {
        return outgoingEdgesOf(vertex).size();
    }

    @Override
    public Set<IRI> outgoingEdgesOf(IRI vertex) {
        Set<IRI> outgoingEdges = new HashSet<>();
        model.getStatements(vertex, null, null).forEach(statement -> outgoingEdges.add(statement.getPredicate()));
        return outgoingEdges;
    }

    @Override
    public boolean removeAllEdges(Collection<? extends IRI> edges) {
        boolean removed = false;
        for (IRI edge : edges) {
            removed |= removeEdge(edge);
        }
        return removed;
    }

    @Override
    public Set<IRI> removeAllEdges(IRI source, IRI target) {
        Set<IRI> removedEdges = new HashSet<>();
        model.getStatements(source, null, target).forEach(statement -> {
            model.remove(statement);
            removedEdges.add(statement.getPredicate());
        });
        return removedEdges;
    }

    @Override
    public boolean removeAllVertices(Collection<? extends IRI> vertices) {
        boolean removed = false;
        for (IRI vertex : vertices) {
            removed |= removeVertex(vertex);
        }
        return removed;
    }

    @Override
    public IRI removeEdge(IRI source, IRI target) {
        IRI removedEdge = null;
        Iterable<Statement> statements = model.getStatements(source, null, target);
        for (Statement statement : statements) {
            removedEdge = statement.getPredicate();
            model.remove(statement);
            break; // Remove the first edge found
        }
        return removedEdge;
    }

    @Override
    public boolean removeEdge(IRI edge) {
        boolean removed = model.remove(null, edge, null);
        return removed;
    }

    @Override
    public boolean removeVertex(IRI vertex) {
        boolean removed = model.remove(vertex, null, null);
        removed |= model.remove(null, null, vertex);
        return removed;
    }

    @Override
    public Set<IRI> vertexSet() {
        Set<IRI> vertices = new HashSet<>();
        model.forEach(statement -> {
            vertices.add((IRI) statement.getSubject());
            vertices.add((IRI) statement.getObject());
        });
        return vertices;
    }

    @Override
    public IRI getEdgeSource(IRI edge) {
        for (Statement statement : model) {
            if (statement.getPredicate().equals(edge)) {
                return (IRI) statement.getSubject();
            }
        }
        return null;
    }

    @Override
    public IRI getEdgeTarget(IRI edge) {
        for (Statement statement : model) {
            if (statement.getPredicate().equals(edge)) {
                return (IRI) statement.getObject();
            }
        }
        return null;
    }

    @Override
    public GraphType getType() {
        return DefaultGraphType.directedSimple().asUnweighted().asUnmodifiable();
    }

    @Override
    public double getEdgeWeight(IRI edge) {
        return 1.0; // Default weight, RDF doesn't have native edge weights
    }

    @Override
    public void setEdgeWeight(IRI edge, double weight) {
        throw new UnsupportedOperationException("Edge weights are not supported in RDF models.");
    }
}
