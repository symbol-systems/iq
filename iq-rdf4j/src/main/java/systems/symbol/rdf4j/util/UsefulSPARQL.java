package systems.symbol.rdf4j.util;

import systems.symbol.rdf4j.NS;

public class UsefulSPARQL {

//    public static final String COUNT_PRED = "SELECT DISTINCT ?p (COUNT(?s) as ?count) WHERE { ?s ?p ?o } GROUP BY ?p;
    public static final String COUNT = "SELECT DISTINCT (COUNT(?s) as ?count) WHERE { ?s ?p ?o }";
    public static final String TYPES_OF = "SELECT DISTINCT ?label ?id WHERE { ?s rdf:type ?id. OPTIONAL {?id rdfs:label ?label.} }";
    public static final String SKOS_CONCEPTS = "SELECT DISTINCT ?label ?id WHERE { ?id ?p ?o. ?id skos:prefLabel ?label. }";
    public static final String SUBJECTS = "SELECT DISTINCT ?id WHERE { ?id ?p ?o.}";
    public static final String PREDICATES = "SELECT DISTINCT ?id WHERE { ?s ?id ?o.}";
    public static final String OBJECTS = "SELECT DISTINCT ?id WHERE { ?s rdf:type ?id }";
    public static final String GRAPHS = "SELECT DISTINCT ?id WHERE { GRAPH ?id { ?s ?p ?o. } }";

    public static final String MODELS = "SELECT DISTINCT ?id ?type WHERE { ?id rdf:value ?text. BIND (datatype(?text) AS <"+ NS.MIME_SPARQL+">) }";
    public static final String SCRIPTS = "SELECT DISTINCT ?id ?type WHERE { ?id rdf:value ?text. BIND (datatype(?text) AS ?type) }";
    public static final String ACTIONS = "SELECT DISTINCT ?id ?type ?text WHERE { ?id rdf:value ?text. BIND (datatype(?text) AS ?type) }";
    public static final String META_ACTIONS = "SELECT DISTINCT ?id ?type ?text ?target WHERE { ?that ?id ?target. ?id rdf:value ?text. BIND (datatype(?text) AS ?type)}";
    public static final String RENDER = "SELECT DISTINCT ?id ?type ?template WHERE { ?id a ?template. ?template rdf:value ?text. BIND (datatype(?text) AS ?type) }";
    public static final String ACL_AUTHORIZED = "SELECT DISTINCT ?id ?agent ?mode WHERE { ?id a acl:Authorization;acl:accessTo ?this;acl:agent ?agent; acl:mode ?mode.}";
    public static final String INDEXER = "SELECT DISTINCT ?label ?definition ?this WHERE { ?this (skos:prefLabel|skos:altLabel|rdf:label|schema:name) ?label .  ?this (skos:definition|schema:description|schema:text|rdfs:comment) ?definition. }";

}
