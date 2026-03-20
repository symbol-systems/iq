package systems.symbol.connect.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Base helper for graph-aware connector modellers.
 *
 * <p>This utility centralizes common connector modelling concerns:
 * graph-scoped writes, T-box term creation, and A-box entity IRI construction.
 */
public abstract class ConnectorGraphModeller {

    private final Model model;
    private final IRI graphIri;
    private final IRI ontologyBaseIri;
    private final IRI entityBaseIri;
    private final ValueFactory valueFactory;

    protected ConnectorGraphModeller(Model model, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
        this.model = Objects.requireNonNull(model, "model");
        this.graphIri = Objects.requireNonNull(graphIri, "graphIri");
        this.ontologyBaseIri = Objects.requireNonNull(ontologyBaseIri, "ontologyBaseIri");
        this.entityBaseIri = Objects.requireNonNull(entityBaseIri, "entityBaseIri");
        this.valueFactory = SimpleValueFactory.getInstance();
    }

    protected final IRI graph() {
        return graphIri;
    }

    protected final IRI ontology(String localName) {
        String base = ontologyBaseIri.stringValue();
        return valueFactory.createIRI(base + localName);
    }

    protected final IRI entity(String namespace, String localName) {
        String ns = Objects.requireNonNull(namespace, "namespace");
        String local = Objects.requireNonNull(localName, "localName");

        StringBuilder iri = new StringBuilder(entityBaseIri.stringValue());
        iri.append(ns);
        if (!ns.endsWith(":")) {
            iri.append(':');
        }
        iri.append(encodeLocal(local));
        return valueFactory.createIRI(iri.toString());
    }

    protected final void add(Resource subject, IRI predicate, Value object) {
        model.add(subject, predicate, object, graphIri);
    }

    protected final void addType(Resource subject, String typeLocalName) {
        add(subject, RDF.TYPE, ontology(typeLocalName));
    }

    protected final void addLiteral(Resource subject, String predicateLocalName, String value) {
        if (value == null) {
            return;
        }
        add(subject, ontology(predicateLocalName), valueFactory.createLiteral(value));
    }

    protected final void addLiteral(Resource subject, String predicateLocalName, boolean value) {
        add(subject, ontology(predicateLocalName), valueFactory.createLiteral(value));
    }

    protected final void addLiteral(Resource subject, String predicateLocalName, int value) {
        add(subject, ontology(predicateLocalName), valueFactory.createLiteral(value));
    }

    protected final void link(Resource subject, String absolutePredicateIri, Resource object) {
        if (object == null) {
            return;
        }
        add(subject, valueFactory.createIRI(absolutePredicateIri), object);
    }

    protected final String encodeLocal(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
