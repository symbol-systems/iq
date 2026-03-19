package systems.symbol.gql;

import systems.symbol.platform.I_Self;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.HashMap;
import java.util.Map;

public class RDFSDomain {
    Map<IRI, String> vocabularies = new HashMap<>();
    Map<IRI, RDFSClass> classes = new HashMap<>();

    public void prefix(String ns, IRI iri) {
        vocabularies.put(iri, ns);
    }

    public RDFSClass addClass(IRI iri, String description) {
        if (classes.containsKey(iri)) {
            RDFSClass rdfsClass = classes.get(iri);
            rdfsClass.description = description;
            return rdfsClass;
        }
        if (description==null) description = iri.getLocalName();
        RDFSClass rdfsClass = new RDFSClass(iri, description);
        classes.put(iri, rdfsClass);
        return rdfsClass;
    }

    public RDFSProperty literal(IRI classIRI, IRI iri, String description) {
        return addClass(classIRI, null).literal(iri, description);
    }

    public RDFSClass object(IRI domainIRI, IRI rangeIRI, String description) {
        RDFSClass range = classes.get(rangeIRI);
        if (range==null)
            range = addClass(rangeIRI, description);
        return addClass(domainIRI, null).object(range);
    }
}
class RDFSClass extends RDFSResource {
    Map<IRI, RDFSProperty> literals = new HashMap<>();
    Map<IRI, RDFSResource> objects = new HashMap<>();

    RDFSClass(IRI iri, String description) {
        super(iri, description);
    }

    public RDFSProperty literal(IRI iri, String description) {
        return literal(iri, XSD.STRING, description);
    }

    public RDFSProperty literal(IRI iri, IRI type, String description) {
        RDFSProperty rdfsProperty = new RDFSProperty(iri, type, description);
        literals.put( iri, rdfsProperty);
        return rdfsProperty;
    }

    public RDFSClass object(RDFSClass c) {
        objects.put(c.getSelf(), c);
        return c;
    }
}
class RDFSProperty extends RDFSResource {
    IRI range = XSD.STRING;

    RDFSProperty(IRI iri, String description) {
        super(iri, description);
    }

    RDFSProperty(IRI iri, IRI type, String description) {
        super(iri, description);
        this.range = type;
    }
}

class RDFSResource implements I_Self {
    IRI iri;
    String description;

    RDFSResource(IRI iri, String description) {
        this.iri=iri;
        this.description=description;
    }

    @Override
    public IRI getSelf() {
        return iri;
    }
}
