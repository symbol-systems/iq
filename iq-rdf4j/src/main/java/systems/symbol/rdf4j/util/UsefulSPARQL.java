package systems.symbol.rdf4j.util;

import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.sparql.JarScriptCatalog;

/**
 * Utility class providing commonly-used SPARQL queries.
 * 
 * As of April 2026, all hardcoded SPARQL queries have been migrated to resource files
 * in /sparql/builtin/ directory. This class now loads them dynamically from JAR resources
 * via JarScriptCatalog, allowing for customization without code changes.
 * 
 * Resource locations (using JarScriptCatalog):
 * - urn:iq:script:builtin:count → /sparql/builtin/count.sparql
 * - urn:iq:script:builtin:types-of → /sparql/builtin/types-of.sparql
 * - urn:iq:script:builtin:skos-concepts → /sparql/builtin/skos-concepts.sparql
 * - urn:iq:script:builtin:subjects → /sparql/builtin/subjects.sparql
 * - urn:iq:script:builtin:predicates → /sparql/builtin/predicates.sparql
 * - urn:iq:script:builtin:objects → /sparql/builtin/objects.sparql
 * - urn:iq:script:builtin:graphs → /sparql/builtin/graphs.sparql
 * - urn:iq:script:builtin:scripts → /sparql/builtin/scripts.sparql
 * - urn:iq:script:builtin:indices → /sparql/builtin/indices.sparql
 * - urn:iq:script:builtin:meta-actions → /sparql/builtin/meta-actions.sparql
 * 
 * @author Symbol Systems
 */
public class UsefulSPARQL {
private static final JarScriptCatalog catalog = new JarScriptCatalog();

// Fallback hardcoded queries (used if resource files not found via JarScriptCatalog)
private static final String COUNT_FALLBACK = "SELECT DISTINCT (COUNT(?s) as ?count) WHERE { ?s ?p ?o }";
private static final String TYPES_OF_FALLBACK = "SELECT DISTINCT ?label ?id WHERE { ?s rdf:type ?id. OPTIONAL {?id rdfs:label ?label.} }";
private static final String SKOS_CONCEPTS_FALLBACK = "SELECT DISTINCT ?label ?id WHERE { ?id ?p ?o. ?id skos:prefLabel ?label. }";
private static final String SUBJECTS_FALLBACK = "SELECT DISTINCT ?id WHERE { ?id ?p ?o.}";
private static final String PREDICATES_FALLBACK = "SELECT DISTINCT ?id WHERE { ?s ?id ?o.}";
private static final String OBJECTS_FALLBACK = "SELECT DISTINCT ?id WHERE { ?s rdf:type ?id }";
private static final String GRAPHS_FALLBACK = "SELECT DISTINCT ?id WHERE { GRAPH ?id { ?s ?p ?o. } }";
private static final String SCRIPTS_FALLBACK = "SELECT DISTINCT ?id ?type ?text WHERE { ?id rdf:value ?text. BIND (datatype(?text) AS ?type) }";
private static final String SPARQLS_FALLBACK = "SELECT DISTINCT ?id ?type WHERE { ?id rdf:value ?text. BIND (datatype(?text) AS <" + NS.MIME_SPARQL + ">) }";
private static final String INDICES_FALLBACK = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nCONSTRUCT { ?this rdf:value ?text } WHERE { ?this rdf:value ?text }";
private static final String META_ACTIONS_FALLBACK = "SELECT DISTINCT ?id ?type ?text ?target WHERE { ?that ?id ?target. ?id rdf:value ?text. BIND (datatype(?text) AS ?type)}";

/**
 * Get COUNT query.
 * Counts the total number of triples in the repository.
 */
public static String getCount() {
return loadQuery("urn:iq:script:builtin:count", COUNT_FALLBACK);
}
public static final String COUNT = getCount();

/**
 * Get TYPES_OF query.
 * Finds all RDF types used in the repository.
 */
public static String getTypesOf() {
return loadQuery("urn:iq:script:builtin:types-of", TYPES_OF_FALLBACK);
}
public static final String TYPES_OF = getTypesOf();

/**
 * Get SKOS_CONCEPTS query.
 * Finds all SKOS concepts with preferred labels.
 */
public static String getSkosconcepts() {
return loadQuery("urn:iq:script:builtin:skos-concepts", SKOS_CONCEPTS_FALLBACK);
}
public static final String SKOS_CONCEPTS = getSkosconcepts();

/**
 * Get SUBJECTS query.
 * Finds all subjects (resources) in the repository.
 */
public static String getSubjects() {
return loadQuery("urn:iq:script:builtin:subjects", SUBJECTS_FALLBACK);
}
public static final String SUBJECTS = getSubjects();

/**
 * Get PREDICATES query.
 * Finds all predicates (properties) in the repository.
 */
public static String getPredicates() {
return loadQuery("urn:iq:script:builtin:predicates", PREDICATES_FALLBACK);
}
public static final String PREDICATES = getPredicates();

/**
 * Get OBJECTS query.
 * Finds all RDF types used as object values.
 */
public static String getObjects() {
return loadQuery("urn:iq:script:builtin:objects", OBJECTS_FALLBACK);
}
public static final String OBJECTS = getObjects();

/**
 * Get GRAPHS query.
 * Finds all named graphs in the repository.
 */
public static String getGraphs() {
return loadQuery("urn:iq:script:builtin:graphs", GRAPHS_FALLBACK);
}
public static final String GRAPHS = getGraphs();

/**
 * Get SCRIPTS query.
 * Finds all stored SPARQL scripts.
 */
public static String getScripts() {
return loadQuery("urn:iq:script:builtin:scripts", SCRIPTS_FALLBACK);
}
public static final String SCRIPTS = getScripts();

/**
 * Get SPARQLS query.
 * Finds all SPARQL scripts (scripts with SPARQL MIME type).
 */
public static String getSparqls() {
return loadQuery("urn:iq:script:builtin:sparqls", SPARQLS_FALLBACK);
}
public static final String SPARQLS = getSparqls();

/**
 * Get INDEXER query.
 * Constructs an index of all resources with their values.
 */
public static String getIndices() {
return loadQuery("urn:iq:script:builtin:indices", INDICES_FALLBACK);
}
public static final String INDEXER = getIndices();

/**
 * Get META_ACTIONS query.
 * Finds all metadata actions.
 */
public static String getMetaActions() {
return loadQuery("urn:iq:script:builtin:meta-actions", META_ACTIONS_FALLBACK);
}
public static final String META_ACTIONS = getMetaActions();

/**
 * Get RENDER query template.
 * This is a hardcoded query for now as it requires special handling.
 */
public static final String RENDER = "SELECT DISTINCT ?id ?type ?template WHERE { ?id a ?template. ?template rdf:value ?text. BIND (datatype(?text) AS ?type) }";

/**
 * SELF query template.
 * Uses Handlebars syntax {{my.self}} for binding injection.
 */
public static String SELF = "CONSTRUCT { ?self ?p ?o. } WHERE { ?self ?p ?o. BIND(<{{my.self}}> AS ?self) }";

/**
 * Loads a query from JAR resources with fallback to hardcoded version.
 * 
 * @param queryIRI The IRI to load from resources
 * @param fallback The fallback query if resource not found
 * @return The query string, or fallback if resource unavailable
 */
private static String loadQuery(String queryIRI, String fallback) {
try {
String query = catalog.getSPARQL(queryIRI);
if (query != null && !query.isEmpty()) {
return query;
}
} catch (Exception e) {
// Log and fall through to fallback
}
return fallback;
}

}
