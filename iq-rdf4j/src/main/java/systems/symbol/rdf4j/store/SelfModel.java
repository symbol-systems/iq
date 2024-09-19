package systems.symbol.rdf4j.store;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.AbstractModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.jetbrains.annotations.NotNull;
import systems.symbol.platform.I_Self;

import java.util.*;

public class SelfModel extends AbstractModel implements I_Self {
RepositoryConnection connection;
private IRI self;

public SelfModel(IRI self, RepositoryConnection connection) {
this.self = self;
this.connection = connection;
}

@Override
public boolean addAll(Collection<? extends Statement> c) {
boolean result = super.addAll(c);
if (result)
connection.commit();
return result;
}

@Override
public IRI getSelf() {
return self;
}

@Override
public void removeTermIteration(Iterator<Statement> iterator, Resource subj, IRI predicate, Value obj,
Resource... contexts) {
// TODO what is this?
this.connection.rollback();
}

@Override
public @NotNull Iterator<Statement> iterator() {
return this.connection.getStatements(null, null, null, getSelf()).iterator();
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
return this.connection.hasStatement(subj, predicate, obj, false, getSelf());
}

@Override
public boolean add(Resource subj, IRI predicate, Value obj, Resource... contexts) {
this.connection.add(subj, predicate, obj, getSelf());
return true;
}

@Override
public boolean remove(Resource subj, IRI predicate, Value obj, Resource... contexts) {
this.connection.remove(subj, predicate, obj, getSelf());
return true;
}

@Override
public Model filter(Resource subj, IRI predicate, Value obj, Resource... contexts) {
Model model = new DynamicModelFactory().createEmptyModel();
RepositoryResult<Statement> statements = this.connection.getStatements(subj, predicate, obj, getSelf());
for (Statement statement : statements) {
model.add(new SelfStatement(self, statement));
}
return model;
}

@Override
public Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
try (RepositoryResult<Statement> result = connection.getStatements(subject, predicate, object, getSelf())) {
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
ns.add(new SimpleNamespace("", self.stringValue()));
ns.add(new SimpleNamespace("self", self.stringValue()));
return ns;
}

// public Bindings getBindings() {
// return new Bindings() {
// }
// }

}
