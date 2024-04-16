package systems.symbol.rdf4j.io;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remodels {
    private static final Logger log = LoggerFactory.getLogger(Remodels.class);
    static DynamicModelFactory dmf = new DynamicModelFactory();

    public static IRI iri(File f) {
        return iri(f.toURI());
    }

    public static IRI iri(URI uri) {
        return Values.iri(uri.toString());
    }

    public static IRI iri(String p) {
        return Values.iri(p);
    }

    public static Model model(String ttl, String baseURL, RDFFormat format)
            throws RDFParseException, UnsupportedRDFormatException, IOException {
        if (baseURL!=null) ttl = "@prefix : <" + baseURL + "> .\n" + ttl;
        return Rio.parse(new StringReader(ttl), baseURL, format);
    }

    public static Model model(RepositoryConnection conn, String ttl, String baseURL, RDFFormat format)
            throws RDFParseException, UnsupportedRDFormatException, IOException {
        StringBuilder rdf = getPrefixes(conn);
        if (baseURL!=null) {
            rdf.append("@prefix : <").append(baseURL).append("> .\n");
            rdf.append("@base <").append(baseURL).append("> .\n");
        }
        rdf.append(ttl);
        return Rio.parse(new StringReader(ttl), baseURL, format);
    }

//    public static Model remodel(RepositoryConnection conn, IRI queryIRI, Map<String, Object> ctx, boolean inferencing)
//            throws IOException {
//        GraphQueryResult results = SPARQLer.query(conn, queryIRI, ctx, inferencing);
//        log.debug("remodel: " + queryIRI + " --> " + (results != null));
//        if (results == null)
//            return null;
//        Model model = dmf.createEmptyModel();
//        for (Statement s : results) {
//            model.add(s);
//        }
//        results.close();
//        return model;
//    }

    public static Model model(GraphQueryResult results)
            throws IOException {
        log.debug("model: " + (results != null));
        if (results == null)
            return null;
        Model model = dmf.createEmptyModel();
        for (Statement s : results) {
            model.add(s);
        }
        results.close();
        return model;
    }

//    public static int infer(RepositoryConnection conn, IRI queryIRI, Map<String, Object> ctx) throws IOException {
//        GraphQueryResult results = SPARQLer.query(conn, queryIRI, ctx, true);
//        log.debug("infer: " + queryIRI + " --> " + (results != null));
//        if (results == null)
//            return -1;
//        int count = 0;
//        conn.begin();
//        for (Statement s : results) {
//            conn.add(s, queryIRI); // add to the queryIRI graph
//            count++;
//        }
//        log.debug("inferred: " + count + " for " + queryIRI);
//        results.close();
//        conn.commit();
//        return count;
//    }

    public static Literal literal(Iterator<Statement> iterator, IRI mimetype) {
        if (mimetype==null) iterator.next();
        HashSet<IRI> mimes = new HashSet<>();
        mimes.add(mimetype);
        return literal(iterator, mimes);
    }

    public static Literal literal(Iterator<Statement> iterator, Set<IRI> mimetypes) {
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            if (statement.getObject() instanceof Literal) {
                Literal literal = (Literal) statement.getObject();
                log.debug("literal.found: " + statement.getSubject() +" --> "+literal+" -> "+mimetypes);
                if (mimetypes.contains(literal.getDatatype())) return literal;
            }
        }
        log.debug("literal.unknown");
        return null;
    }

    public static StringBuilder getPrefixes(RepositoryConnection connection) {
        StringBuilder prefixes = new StringBuilder();

        RepositoryResult<Namespace> namespaces = connection.getNamespaces();

        while (namespaces.hasNext()) {
            Namespace namespace = namespaces.next();
            String p = namespace.getPrefix();
            if (p.length() > 1) {
                prefixes.append("@prefix ")
                        .append(p)
                        .append(": <")
                        .append(namespace.getName())
                        .append("> .\n");
            }
        }
        return prefixes;
    }
}
