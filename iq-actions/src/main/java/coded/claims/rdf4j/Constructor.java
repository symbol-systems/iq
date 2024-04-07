package systems.symbol.rdf4j;
/*
 *  systems.symbol - Proprietary License
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 *
 *  25/04/2014 re-licensed BSD License [Symbol Systems].
 */

import systems.symbol.rdf4j.util.RDFPrefixer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.n3.N3Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * systems.symbol (c) 2010-2013
 *
 *
 */

public class Constructor {
	private static final Logger log = LoggerFactory.getLogger(Constructor.class);
	RepositoryConnection connection = null;
boolean isInferred = false;
	IRI context;

	public Constructor(RepositoryConnection connection, IRI context, boolean isInferred) {
		this.connection = connection;
		this.context=context;
		this.isInferred=isInferred;
	}


	public int apply(String query, RepositoryConnection to, boolean strictlyLiteral) throws RepositoryException, QueryEvaluationException, MalformedQueryException {
		if (!query.contains("CONSTRUCT")) {
			throw new MalformedQueryException("Not a CONSTRUCT query: "+query);
		}
		if (!query.contains("PREFIX ")) {
			query = RDFPrefixer.getSPARQLPrefix(connection)+"\n"+query;
		}
		log.trace("iq.sparql.execute:apply: "+query);
		GraphQuery graphQuery = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
log.debug("GraphQuery: "+query+"\n -> from: "+graphQuery.getClass());
		GraphQueryResult result = graphQuery.evaluate();
		int count = apply(result, to, strictlyLiteral);
		result.close();
		return count;
	}

	public int apply(GraphQueryResult result, RepositoryConnection to, boolean strictlyLiteral) throws RepositoryException, QueryEvaluationException, MalformedQueryException {
		int count = 0;
		to.begin();
		ValueFactory vf = to.getRepository().getValueFactory();
		while (result.hasNext()) {
			Statement stmt = result.next();
			if (strictlyLiteral && stmt.getObject().isLiteral()) {
				to.remove(stmt.getSubject(), stmt.getPredicate(), null);
			}
			Statement new_stmt = vf.createStatement(stmt.getSubject(), stmt.getPredicate(), stmt.getObject(), context);
//			log.debug("---> "+new_stmt+" @ "+context);
			to.add(new_stmt, context);
			count++;
		}
log.debug("inferred "+count+"  into: "+context.toString());

		to.commit();
		return count;
	}


	public void write(String query, File file) throws IOException, QueryEvaluationException, RDFHandlerException {
		N3Writer n3_writer = new N3Writer(new FileWriter(file));
		write(query, n3_writer);
	}

	public void write(String query, N3Writer n3_writer) throws QueryEvaluationException, RDFHandlerException {
		if (query==null || query.isEmpty()) throw new QueryEvaluationException("Missing query");
			GraphQuery gq = connection.prepareGraphQuery(QueryLanguage.SPARQL, query);
			gq.evaluate(n3_writer);
	}

	public void copyNamespaces(RepositoryConnection to) throws RepositoryException {
		copyNamespaces(connection, to);
	}

public static void copyNamespaces(RepositoryConnection from, RepositoryConnection to) throws RepositoryException {
defaultNamespaces(to);

RepositoryResult<Namespace> namespaces = from.getNamespaces();

for(Namespace namespace:namespaces.asList()) {
if (!namespace.getPrefix().isEmpty())
to.setNamespace(namespace.getPrefix(), namespace.getName());
}
}

public static void defaultNamespaces(RepositoryConnection to) throws RepositoryException {
to.setNamespace("rdf", RDF.NAMESPACE);
to.setNamespace("rdfs", RDFS.NAMESPACE);
to.setNamespace("owl", OWL.NAMESPACE);
to.setNamespace("skos", SKOS.NAMESPACE);
to.setNamespace("dc", DC.NAMESPACE);
		to.setNamespace("shacl", SHACL.NAMESPACE);
to.setNamespace("xsd", XSD.NAMESPACE);
}
}
