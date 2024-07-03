package systems.symbol.realm;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.finder.FactFinder;
import systems.symbol.finder.I_Finder;
import systems.symbol.finder.IndexHelper;
import systems.symbol.platform.IQ_NS;
import systems.symbol.platform.I_Contents;
import systems.symbol.rdf4j.IRIs;
import systems.symbol.rdf4j.sparql.IQScriptCatalog;

import java.util.Set;

public class Facts {

public static Set<IRI> find(Model model, Resource agent, IRI follow) {
return find(model, agent, new IRIs(), false, follow);
}

public static Set<IRI> find(Model model, Resource self, Set<IRI> found, boolean recurse, IRI follow) {
if (!self.isIRI()) return found;
if (found.contains((IRI)self)) return found;
found.add((IRI)self);
Iterable<Statement> trusted = model.getStatements(self, follow, null);
for (Statement st : trusted) {
Value thing = st.getObject();
if (thing!=null && thing.isIRI()) {
IRI resource = Values.iri(thing.stringValue());
find(model, resource, found, recurse, follow);
}
}
return found;
}

public static void copy(Model from, Iterable<IRI> found, Model to) {
for(IRI todo: found) {
Iterable<Statement> trusted = from.getStatements(todo, null, null);
for (Statement st : trusted) {
to.add(st);
}
}
}

public static void meld(Model from, Iterable<IRI> found, Model to) {
meld(from, found, to, RDF.VALUE);
}

public static void meld(Model from, Iterable<IRI> found, Model to, IRI p) {
for(IRI todo: found) {
Iterable<Statement> trusted = from.getStatements(todo, p, null);
for (Statement st : trusted) {
to.add(st);
}
}
}

public static void index(RepositoryConnection connection, IRI self, IRI query, FactFinder finder) {
I_Contents queries = new IQScriptCatalog(self, connection);
Literal sparql = queries.getContent(query, null);
if (sparql == null) return;
IndexHelper.index(finder, connection.prepareTupleQuery(sparql.stringValue()));
}

public static void index(I_Finder finder, Model model, IRI self) {
Set<IRI> found = find(model, self, new IRIs(), false, IQ_NS.TRUSTS);
find(model, self, found, false, IQ_NS.TRUSTS);
find(model, self, found, false, IQ_NS.KNOWS);
index(finder, model, found, RDF.VALUE);
}

public static void index(I_Finder finder, Model model, IRI self, IRI follow) {
index(finder, model, self, follow, RDF.VALUE);
}

public static void index(I_Finder finder, Model model, IRI self, IRI follow, IRI value) {
Iterable<IRI> found = find(model, self, new IRIs(), false, follow);
index(finder, model, found, value);
 }

public static void index(I_Finder finder, Model model, Iterable<IRI> found, IRI value) {
for(IRI todo: found) {
StringBuilder content = new StringBuilder();
Iterable<Statement> statements = model.getStatements(todo, value, null);
for (Statement st : statements) {
content.append(st.getObject().stringValue());
}
finder.store(todo.stringValue(), content.toString());
}
}
}
