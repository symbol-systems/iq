package systems.symbol.rdf4j.util;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import systems.symbol.platform.IQ_NS;

import java.util.HashMap;
import java.util.Map;

/**
 * ORIGINAL: (c) Symbol Systems, 2009-2015.
 *
 * symbol.systems (c) 2011-2013
 * Module: systems.symbol.util
 * @author Symbol Systems
 * Date  : 7/05/2014
 * Time  : 1:16 PM
 */
public class RDFPrefixer {

	public static void defaultNamespaces(RepositoryConnection to) throws RepositoryException {
		Map<String, String> ns = defaults();
		for (String n : ns.keySet()) {
			to.setNamespace(n, ns.get(n));
		}
	}

	public static void defaultNamespaces(Model to) throws RepositoryException {
		Map<String, String> ns = defaults();
		for (String n : ns.keySet()) {
			to.setNamespace(n, ns.get(n));
		}
	}

	public static Map<String, String> simple() {
		Map<String, String> ns = new HashMap<>();
		ns.put("rdf", RDF.NAMESPACE);
		ns.put("rdfs", RDFS.NAMESPACE);
		ns.put("skos", SKOS.NAMESPACE);
		return ns;
	}

	public static Map<String, String> defaults() {
		Map<String, String> ns = new HashMap<>();
		ns.put("rdf", RDF.NAMESPACE);
		ns.put("rdfs", RDFS.NAMESPACE);
		ns.put("owl", OWL.NAMESPACE);
		ns.put("skos", SKOS.NAMESPACE);
		ns.put("dc", DC.NAMESPACE);
		ns.put("dct", DCTERMS.NAMESPACE);
		ns.put("dcat", DCAT.NAMESPACE);
		ns.put("xsd", XSD.NAMESPACE);
		ns.put("iq", IQ_NS.IQ);
		ns.put("my", IQ_NS.MY);
		ns.put("prov", PROV.NAMESPACE);
		ns.put("void", VOID.NAMESPACE);
		ns.put("schema", "http://schema.org/");
		ns.put("cnt", "http://www.w3.org/2011/content#");
		return ns;
	}

	public static Map<String, String> defaults(RepositoryConnection repositoryConnection) {
		Map<String, String> ns = RDFPrefixer.defaults();
		RepositoryResult<Namespace> all_namespaces = repositoryConnection.getNamespaces();
		while (all_namespaces.hasNext()) {
			Namespace _ns = all_namespaces.next();
			if (!_ns.getPrefix().isEmpty()) {
				ns.put(_ns.getPrefix(), _ns.getName());
			}
		}
		return ns;
	}

	public static String getSPARQLPrefix(RepositoryConnection repositoryConnection) throws RepositoryException {
		Map<String, String> ns = RDFPrefixer.defaults(repositoryConnection);
		StringBuilder names$ = new StringBuilder();
		for (String n : ns.keySet()) {
			String v = ns.get(n);
			names$.append("PREFIX ").append(n).append(": <").append(v).append(">\n");
		}
		return names$.toString();
	}

	public static StringBuilder getPrefix(RepositoryConnection repositoryConnection) throws RepositoryException {
		Map<String, String> ns = RDFPrefixer.defaults(repositoryConnection);
		StringBuilder names$ = new StringBuilder();
		for (String n : ns.keySet()) {
			String v = ns.get(n);
			names$.append("@prefix ").append(n).append(": <").append(v).append(">.\n");
		}
		return names$;
	}


	public static StringBuilder toPrefix(Map<String, String> ns) throws RepositoryException {
		StringBuilder names$ = new StringBuilder();
		names$.append("|@prefix|namespace url|");
		for (String n : ns.keySet()) {
			names$.append("| ").append(n).append(": |").append(ns.get(n)).append("|\n");
		}
		return names$;
	}

}
