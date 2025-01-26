package systems.symbol.rdf4j;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class Facts {
    static protected DynamicModelFactory dmf = new DynamicModelFactory();

    public static Set<IRI> find(Model model, Resource agent, IRI follow) {
        return find(model, agent, new IRIs(), false, follow);
    }

    public static Set<IRI> find(Model model, Resource self, Set<IRI> found, boolean recurse, IRI follow) {
        if (!self.isIRI())
            return found;
        if (found.contains((IRI) self))
            return found;
        found.add((IRI) self);
        Iterable<Statement> facts = model.getStatements(self, follow, null);
        for (Statement st : facts) {
            Value thing = st.getObject();
            if (thing != null && thing.isIRI()) {
                IRI resource = Values.iri(thing.stringValue());
                find(model, resource, found, recurse, follow);
            }
        }
        return found;
    }

    public static void copy(Model from, Iterable<IRI> found, Model to) {
        for (IRI todo : found) {
            Iterable<Statement> trusted = from.getStatements(todo, null, null);
            for (Statement st : trusted) {
                to.add(st);
            }
        }
    }

    public static Model clone(Repository repo) {
        DynamicModel model = dmf.createEmptyModel();
        try (RepositoryConnection conn = repo.getConnection()) {
            RepositoryResult<Statement> statements = conn.getStatements(null, null, null);
            for (Statement s : statements) {
                model.add(s);
            }
        }
        return model;
    }

    public static void meld(Model from, Iterable<IRI> found, Model to) {
        meld(from, found, to, RDF.VALUE);
    }

    public static void meld(Model from, Iterable<IRI> found, Model to, IRI p) {
        for (IRI todo : found) {
            Iterable<Statement> trusted = from.getStatements(todo, p, null);
            for (Statement st : trusted) {
                to.add(st);
            }
        }
    }

    public static String[] toStrings(Collection<Resource> items) {
        if (items == null || items.isEmpty()) {
            return new String[0];
        }
        return items.stream()
                .map(Resource::stringValue)
                .toArray(String[]::new);
    }

    public static String toString(Collection<Resource> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(Resource::stringValue)
                .collect(Collectors.joining(","));
    }

}
