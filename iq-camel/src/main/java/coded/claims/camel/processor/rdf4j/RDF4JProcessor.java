package systems.symbol.camel.processor.rdf4j;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.support.ExchangeHelper;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;

/**
 * systems.symbol (c) 2014-2023
 * Module: systems.symbol.camel.component.sparql
 * @author Symbol Systems
 * Date  : 25/06/2014
 * Time  : 11:33 AM
 */
public class RDF4JProcessor implements Processor {
	private static final Logger log = LoggerFactory.getLogger(RDF4JProcessor.class);

	boolean inferred = true;
	private int maxQueryTime = -1;
	String outputType, queryType;
	Repository repository;

	public RDF4JProcessor(Repository repository, String type, Boolean isInferred, Integer maxQueryTime, String contentType) {
		this.repository=repository;
		this.queryType=type;
		this.inferred=isInferred;
		this.maxQueryTime=maxQueryTime;
		this.outputType = contentType;
		log.debug("SesameProcessor: "+ outputType);

	}

	@Override
	@Handler
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		Map<String,Object> headers = in.getHeaders();

		Message out = exchange.getOut();
		out.setHeaders(headers);

		String sparql = null, contentType = this.outputType;
		// SPARQL query is specified in message Body not in declaration

		contentType = contentType==null? ExchangeHelper.getContentType(exchange):contentType;
		if (contentType==null||contentType.isEmpty()) {
			log.debug("Accept-Types:"+headers.get("Accept"));
			contentType = (String) headers.get("Accept");
		}

		sparql = in.getBody(String.class);

		try (RepositoryConnection connection = repository.getConnection()) {
			if ("select".equalsIgnoreCase(queryType)) {
				if ( isMissing(sparql) ) throw new MalformedQueryException("Missing SPARQL SELECT query");
				processSelect(connection, headers, contentType, sparql, out);
			} else if ("load".equalsIgnoreCase(queryType)) {
				if ( isMissing(sparql) ) throw new MalformedQueryException("Missing RDF statements");
				processLoad(connection, headers, contentType, sparql, out);
			} else if ("construct".equalsIgnoreCase(queryType) || "describe".equalsIgnoreCase(queryType)) {
				if ( isMissing(sparql) ) throw new MalformedQueryException("Missing SPARQL CONSTRUCT|DESCRIBE query");
				processConstruct(connection, headers, contentType, sparql, out);
			} else if ("ask".equalsIgnoreCase(queryType)) {
				if ( isMissing(sparql) ) throw new MalformedQueryException("Missing SPARQL ASK query");
				processAsk(connection, sparql, out);
			} else {
				throw new MalformedQueryException("Missing SPARQL operation");
			}
		}
	}
	
	boolean isMissing(String sparql) {
		return sparql==null||sparql.isEmpty();
	}

	private void processAsk(RepositoryConnection connection, String sparql, Message out) {
		BooleanQuery query = connection.prepareBooleanQuery(QueryLanguage.SPARQL, sparql);
		query.setIncludeInferred(isInferred());
		if (maxQueryTime>0) query.setMaxExecutionTime(getMaxQueryTime());
		boolean graphQueryResult = query.evaluate();
		out.setBody(graphQueryResult);
	}

	private void processLoad(RepositoryConnection connection, Map<String, Object> headers, String contentType, String triples, Message out) throws RepositoryException, RDFParseException, IOException {
		RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(contentType).orElse(null);
		if (rdfFormat==null) {
			log.debug("Unknown format: " + contentType);
			return;
		}
		String baseURI = (String) headers.get("Sesame-Context");
		if (baseURI==null||baseURI.isEmpty()) baseURI = "bean:"+getClass().getCanonicalName();
		connection.begin();
		log.debug("LOADING: " + contentType+" -> "+rdfFormat+" into "+baseURI);
		connection.add( new StringReader(triples), baseURI, rdfFormat );
		connection.commit();
	}

	private void processSelect(RepositoryConnection connection, Map<String, Object> headers, String contentType, String sparql, Message out) throws RepositoryException, QueryResultHandlerException, MalformedQueryException, QueryEvaluationException, IOException {
		TupleQueryResultFormat parserFormatForMIMEType = QueryResultIO.getParserFormatForMIMEType(contentType, null);
		if (parserFormatForMIMEType!=null) {
			headers.put("Content-Type", parserFormatForMIMEType.getDefaultMIMEType()+";"+parserFormatForMIMEType.getCharset());
			StringWriter stringWriter = handle(connection, sparql, parserFormatForMIMEType);
			log.trace("TUPLES: " + stringWriter.toString());
			out.setBody(stringWriter.toString());
		} else {
			// unknown-type, internal
			java.util.Collection<java.util.Map> results = systems.symbol.rdf4j.util.SesameHelper.toMapCollection(connection, sparql);
			log.debug("COLLECTION: " + results);
			out.setBody(results);
		}
	}

	public StringWriter handle(RepositoryConnection connection, String sparql, TupleQueryResultFormat parserFormatForMIMEType) throws MalformedQueryException, RepositoryException, QueryResultHandlerException, QueryEvaluationException, IOException {
		StringWriter stringWriter = new StringWriter();
		OutputStream out = new org.apache.commons.io.output.WriterOutputStream(stringWriter);
		handle(connection, sparql, out, parserFormatForMIMEType);
		out.close();
		return stringWriter;
	}

	public void handle(RepositoryConnection connection, String sparql, OutputStream out, TupleQueryResultFormat parserFormatForMIMEType) throws MalformedQueryException, RepositoryException, QueryResultHandlerException, QueryEvaluationException {
		// handle query and result set
		TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
		tupleQuery.setIncludeInferred(isInferred());
		if (maxQueryTime>0) tupleQuery.setMaxQueryTime(getMaxQueryTime());

		TupleQueryResultWriter resultWriter = QueryResultIO.createWriter(parserFormatForMIMEType, out);
		resultWriter.startQueryResult(new ArrayList());
		tupleQuery.evaluate(resultWriter);
		resultWriter.endQueryResult();
	}

	public GraphQueryResult executeGraphQuery(RepositoryConnection connection, String sparql) throws MalformedQueryException, RepositoryException, QueryResultHandlerException, QueryEvaluationException, IOException, RDFHandlerException {
		// handle query and result set
		GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql);
		query.setIncludeInferred(isInferred());
		if (maxQueryTime>0) query.setMaxExecutionTime(getMaxQueryTime());
		GraphQueryResult graphQueryResult = query.evaluate();
		return graphQueryResult;
	}

	public boolean isInferred() {
		return inferred;
	}

	public int getMaxQueryTime() {
		return maxQueryTime;
	}

}
