package systems.symbol.rdf4j;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.HashSet;
import java.util.List;

public class IRIs extends HashSet<IRI> {
    private static final long serialVersionUID = 12312381388131L;

    public IRIs() {
    }

    public IRIs(IRI iri) {
        add(iri);
    }

    public static IRIs forLabels() {
        IRIs labels = new IRIs();
        labels.add(SKOS.PREF_LABEL);
        labels.add(SKOS.ALT_LABEL);
        labels.add(DCTERMS.TITLE);
        labels.add(RDFS.LABEL);
        return labels;
    }

    public static IRIs forLinks() {
        IRIs links = new IRIs();
        links.add(SKOS.IN_SCHEME);
        links.add(OWL.SAMEAS);
        return links;
    }

    public static IRIs forSPARQL() {
        IRIs links = new IRIs();
        links.add(SimpleValueFactory.getInstance().createIRI(NS.MIME_SPARQL));
        return links;
    }

    public static IRIs A() {
        return new IRIs(RDF.TYPE);
    }

    public static IRIs ISA() {
        IRIs iris = new IRIs(RDF.TYPE);
        iris.add(RDFS.SUBCLASSOF);
        return iris;
    }

    public void add(ValueFactory vf, List<String> iris) {
        for(String iri: iris) {
            add( vf, iri);
        }
    }

    public void add(ValueFactory vf, String iri) {
        add( vf.createIRI(iri));
    }
}
