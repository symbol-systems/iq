package systems.symbol;

import systems.symbol.platform.I_Self;

/**
 * The {@code COMMONS} interface provides common constants used in IQ.
 * These constants include codenames, namespaces, and MIME types.
 */
public interface COMMONS {

	// Codename for the IQ operating environment
	String IQ = I_Self.name();

	// Namespace prefixes
	// MIME types
	String MIME_GROOVY = "application/x-groovy";
	String MIME_PLAIN = "text/plain";
	String MIME_HBS = "text/x-handlebars";
	String MIME_JSON = "application/json";
	String MIME_SPARQL = "application/x-sparql-query";
	String MIME_HTML = "text/html";
	String MIME_JAVA = "text/x-java-source";
	String MIME_GRAPHQL = "text/graphql";
	String MIME_XHTML = "application/xhtml+xml";

	// Test namespaces
	String GG_TEST = "https://test.symbol.systems/cases#";
}
