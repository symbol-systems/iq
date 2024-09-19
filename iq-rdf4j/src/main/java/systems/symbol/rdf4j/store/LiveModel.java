package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.AbstractModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class LiveModel extends AbstractModel {
    private static final long serialVersionUID = 1L;
    protected RepositoryConnection connection;

    public LiveModel(RepositoryConnection connection) {
        this.connection = connection;
    }

    @Override
    public void removeTermIteration(Iterator<Statement> iterator, Resource subj, IRI predicate, Value obj,
            Resource... contexts) {
    }

    @Override
    public @NotNull Iterator<Statement> iterator() {
        return this.connection.getStatements(null, null, null).iterator();
    }

    @Override
    public int size() {
        return (int) this.connection.size();
    }

    @Override
    public void setNamespace(Namespace namespace) {
        this.connection.setNamespace(namespace.getPrefix(), namespace.getName());
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        this.connection.removeNamespace(prefix);
        return Optional.empty();
    }

    @Override
    public boolean contains(Resource subj, IRI predicate, Value obj, Resource... contexts) {
        return this.connection.hasStatement(subj, predicate, obj, false, contexts);
    }

    @Override
    public boolean add(Resource subj, IRI predicate, Value obj, Resource... contexts) {
        this.connection.add(subj, predicate, obj, contexts);
        return true;
    }

    @Override
    public boolean remove(Resource subj, IRI predicate, Value obj, Resource... contexts) {
        this.connection.remove(subj, predicate, obj, contexts);
        return true;
    }

    @Override
    public Model filter(Resource subj, IRI predicate, Value obj, Resource... contexts) {
        Model model = new DynamicModelFactory().createEmptyModel();
        RepositoryResult<Statement> statements = this.connection.getStatements(subj, predicate, obj, contexts);
        for (Statement statement : statements) {
            model.add(statement);
        }
        return model;
    }

    @Override
    public Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
        try (RepositoryResult<Statement> result = connection.getStatements(subject, predicate, object, contexts)) {
            Collection<Statement> found = new ArrayList<>();
            for (Statement s : result) {
                found.add(s);
            }
            return found;
        }
    }

    @Override
    public Set<Namespace> getNamespaces() {
        RepositoryResult<Namespace> namespaces = this.connection.getNamespaces();
        Set<Namespace> ns = new HashSet<>();
        while (namespaces.hasNext()) {
            ns.add(namespaces.next());
        }
        return ns;
    }
}
