package systems.symbol.platform;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SubjectiveModel implements Model {
private final IRI self;
Model model;

public SubjectiveModel(Model model, IRI self) {
this.model = model;
this.self = self;
}
@Override
public Model unmodifiable() {
return model.unmodifiable();
}

@Override
public void setNamespace(Namespace namespace) {
model.setNamespace(namespace);
}

@Override
public Optional<Namespace> removeNamespace(String prefix) {
return model.removeNamespace(prefix);
}

@Override
public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
return model.contains( subj, pred, obj, subjective(contexts));
}

@Override
public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
if (subj instanceof IRI && !isSubjective((IRI)subj)) return false;
return model.add(subj,pred,obj,subjective(contexts));
}

@Override
public boolean clear(Resource... context) {
return model.clear(subjective(context));
}

@Override
public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
return model.remove(subj, pred,obj, subjective(contexts));
}

@Override
public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
return model.filter(subj, pred,obj, subjective(contexts));
}

@Override
public Set<Resource> subjects() {
return model.subjects();
}

@Override
public Set<IRI> predicates() {
return model.predicates();
}

@Override
public Set<Value> objects() {
return model.objects();
}

@Override
public int size() {
return model.size();
}

@Override
public boolean isEmpty() {
return model.isEmpty();
}

@Override
public boolean contains(Object o) {
return model.contains(o);
}

@NotNull
@Override
public Iterator<Statement> iterator() {
return model.iterator();
}

@NotNull
@Override
public Object[] toArray() {
return model.toArray();
}

@NotNull
@Override
public <T> T[] toArray(@NotNull T[] ts) {
return model.toArray(ts);
}

@Override
public boolean add(Statement statement) {
return add(statement.getSubject(), statement.getPredicate(), statement.getObject(), self);
}

@Override
public boolean remove(Object o) {
return model.remove(o);
}

@Override
public boolean containsAll(@NotNull Collection<?> collection) {
return model.containsAll(collection);
}

@Override
public boolean addAll(@NotNull Collection<? extends Statement> collection) {
return model.addAll(collection);
}

@Override
public boolean retainAll(@NotNull Collection<?> collection) {
return model.retainAll(collection);
}

@Override
public boolean removeAll(@NotNull Collection<?> collection) {
return model.removeAll(collection);
}

@Override
public void clear() {
model.clear();
}

@Override
public Set<Namespace> getNamespaces() {
return model.getNamespaces();
}

public boolean isSubjective(IRI subj) {
return subj!=null && subj.stringValue().startsWith(self.stringValue());
}

public Resource[] subjective(Resource... contexts) {
List<Resource> subjective = new ArrayList<>(Arrays.asList(contexts));
if (!subjective.contains(self))  subjective.add(self);
return subjective.toArray(new Resource[0]);
}

}
