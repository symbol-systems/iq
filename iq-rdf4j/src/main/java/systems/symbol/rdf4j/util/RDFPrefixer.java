package systems.symbol.rdf4j.util;

import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * ORIGINAL: (c) Symbol Systems, 2009-2015.
 *
 * systems.symbol (c) 2011-2013
 * Module: systems.symbol.util
 * @author Symbol Systems
 * Date  : 7/05/2014
 * Time  : 1:16 PM
 */
public class RDFPrefixer {


	public static void defaultNamespaces(RepositoryConnection to) throws RepositoryException {
		Map<String, String> ns = defaults();
		for(String n: ns.keySet()) {
			to.setNamespace(n, ns.get(n));
		}
	}

	public static void defaultNamespaces(Model to) throws RepositoryException {
		Map<String, String> ns = defaults();
		for(String n: ns.keySet()) {
			to.setNamespace(n, ns.get(n));
		}
	}

	public static Map<String,String> defaults() {
		Map<String,String> ns = new HashMap<>();
		ns.put("rdf", RDF.NAMESPACE);
		ns.put("rdfs", RDFS.NAMESPACE);
		ns.put("owl", OWL.NAMESPACE);
		ns.put("skos", SKOS.NAMESPACE);
		ns.put("dc", DC.NAMESPACE);
		ns.put("dct", DCTERMS.NAMESPACE);
		ns.put("xsd", XSD.NAMESPACE);
		ns.put("iq", COMMONS.IQ_NS);
		ns.put("schema", "http://schema.org/");
		ns.put("cnt", "http://www.w3.org/2011/content#");
		return ns;
	}


	public static Map<String,String> defaults(RepositoryConnection repositoryConnection) {
		Map<String,String> ns = RDFPrefixer.defaults();
		RepositoryResult<Namespace> all_namespaces = repositoryConnection.getNamespaces();
		while(all_namespaces.hasNext() ) {
			Namespace _ns = all_namespaces.next();
			if (!_ns.getPrefix().isEmpty()) {
				ns.put( _ns.getPrefix(), _ns.getName());
			}
		}
		return ns;
	}

	public static String getSPARQLPrefix(RepositoryConnection repositoryConnection) throws RepositoryException {
		Map<String,String> ns = RDFPrefixer.defaults(repositoryConnection);
		StringBuilder names$ = new StringBuilder();
		for(String n: ns.keySet()) {
			String v = ns.get(n);
			names$.
			append("PREFIX ").
			append(n).
			append(": <").
			append(v).
			append(">\n");
		}
		return names$.toString();
	}

	public static String addN3Prefix(RepositoryConnection repositoryConnection, String n3s) throws RepositoryException {
		Map<String,String> ns = RDFPrefixer.defaults(repositoryConnection);

		StringBuilder names$ = new StringBuilder();

		for(String n: ns.keySet()) {
			String v = ns.get(n);
			names$.
				append("@prefix ").
				append(n).
				append(": <").
				append(v).
				append(">.\n");
		}
		names$.append(n3s);
		return names$.toString();
	}

	public static String toTurtle(Set<Namespace> namespaces) {
		StringBuilder namespaceBuilder = new StringBuilder();
		for (Namespace namespace : namespaces) {
			namespaceBuilder.append("@prefix ")
					.append(namespace.getPrefix())
					.append(": <")
					.append(namespace.getName())
					.append(">\n");
		}
		return namespaceBuilder.toString();
	}

	public static IRI toIRI(Model model, String k) {
		int ix = k.indexOf(":");
		if (ix<0) return null;
		String p = k.substring(0, ix);
		return Values.iri(model.getNamespace(p)+k.substring(ix+1));
	}

	public static IRI toIRI(Model model, IRI self, String k) {
		IRI property = RDFPrefixer.toIRI(model, k);
		if (property != null) return property;
		String s = self.stringValue();
		if (s.charAt(s.length()-1) == '#')  return Values.iri(s + k);
		return Values.iri(s + "#" + k);
	}
}
