package systems.symbol.rdf4j.util;

import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.NS;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * symbol.systems (c) 2014
 * Module: systems.symbol.rdf4j.util
 * @author Symbol Systems
 * Date  : 17/06/2014
 * Time  : 10:16 PM
 */
public class RDFHelper {
    private static final Logger log = LoggerFactory.getLogger(RDFHelper.class);

	protected static ValueFactory vf = SimpleValueFactory.getInstance();
    public static IRI toIRI(RepositoryConnection c, String s) {
		return c.getValueFactory().createIRI(s);
	}

	public static IRI toIRI(TripleSource ts, String s) {
		return ts.getValueFactory().createIRI(s);
	}

	public static IRI toIRI(TripleSource ts, NS ns, String s) {
		if (ns.contains(s)) {
			return ts.getValueFactory().createIRI(s);
		}
		return null;
	}

	public static StringBuilder toSPARQLPrefix(RepositoryConnection connection) throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException {
		RepositoryResult<Namespace> namespaces = connection.getNamespaces();
		StringBuilder namespace$ = new StringBuilder();
		while(namespaces.hasNext()) {
			Namespace namespace = namespaces.next();
			namespace$.append("PREFIX ").append(namespace.getPrefix()).append(": <").append(namespace.getName()).append( ">\n");
		}
		log.trace("SPARQL prefices: " + namespace$);
		return namespace$;
	}

	public static CloseableIteration<? extends Statement, QueryEvaluationException> find(TripleSource tripleSource, IRI s, IRI p, Value v, IRI c) {
    	if (c==null)
			return tripleSource.getStatements(s, p, v);
		return tripleSource.getStatements(s, p, v, c);
	}

	public static void save(RepositoryConnection connection, IRI subject, Map<IRI, Value> model, IRI context) {
		ValueFactory vf = connection.getValueFactory();
		connection.begin();
		for(IRI p: model.keySet()) {
			Value value = model.get(p);
			Statement statement = vf.createStatement(subject, p, value, context);
			connection.add(statement, context);
		}
		connection.commit();
	}

	public static Literal label(Model triples, Resource s) {
		Iterable<Statement> models = triples.getStatements(s, RDFS.LABEL, null);
		Literal found = null;
        for (Statement next : models) {
            if (found == null && next.getObject().isLiteral()) {
                found = (Literal) next.getObject();
            }
        }
		return found;
	}
}
