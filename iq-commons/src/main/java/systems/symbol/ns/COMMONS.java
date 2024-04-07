package systems.symbol.ns;
/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
/**
 * systems.symbol (c) 2010-2013
 *
 * Define common namespaces for internal vocabularies
 *
 * 1st May 2014- Symbol Systems 
 *
 */
public interface COMMONS {

	String IQ_NS = "urn:systems.symbol:v0:iq:";
	String IQ_NS_TEST = "https://test.symbol.systems/cases#";

// Common Vocabularies;

	String XSD = "http://www.w3.org/2001/XMLSchema#";

String MIME_TYPE = "http://www.iana.org/assignments/media-types/";

String MIME_GROOVY = MIME_TYPE+ "application/x-groovy";
String MIME_PLAIN = MIME_TYPE+"text/plain";
	String MIME_HBS = MIME_TYPE+"text/x-handlebars";
String MIME_JSON = MIME_TYPE+ "application/json";
String MIME_SPARQL = MIME_TYPE+"application/x-sparql-query";
	String MIME_HTML = MIME_TYPE+ "text/html";
	String MIME_GRAPHQL = MIME_TYPE+ "text/graphql";

	String MIME_JAVASCRIPT = MIME_TYPE+ "application/javascript";
	String MIME_PROPERTIES = MIME_TYPE+ "text/x-java-properties";
	String MIME_SQL = MIME_TYPE+"text/x-sql";
	String MIME_JSON_LD = MIME_TYPE+"application/ld+json";
	String MIME_RDFXML = MIME_TYPE+"application/rdf+xml";

String MIME_XHTML = MIME_TYPE+ "application/xhtml+xml";
String MIME_CSV = MIME_TYPE+"text/csv";
String MIME_XML = MIME_TYPE+"application/xml";
	
	String STRING = XSD+"string";
	String INTEGER = XSD+"integer";
	String DOUBLE= XSD+"double";
	String FLOAT = XSD+"float";
	String DATE = XSD+"date";
	String ANY_URI = XSD+"anyURI";
	String DATE_TIME = XSD+"dateTime";
	String BOOLEAN = XSD+"boolean";

	String CNT = "http://www.w3.org/2011/content#";

	//	String LIST = RDF+"list";

}
