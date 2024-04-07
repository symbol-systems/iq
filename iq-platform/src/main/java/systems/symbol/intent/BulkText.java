package systems.symbol.intent;

import systems.symbol.annotation.RDF;
import systems.symbol.io.Fingerprint;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import systems.symbol.render.HBSRenderer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BulkText extends IQIntent {
    protected IRI templateMime, newPredicate;
    protected boolean force = false;

    protected BulkText() {}

    public BulkText(IQ iq) throws IOException {
        init(iq, null, null, templateMime, newPredicate, force);
    }

    public BulkText(IQ iq, IRI templateMime, IRI newPredicate) throws IOException {
        init(iq, null, null, templateMime, newPredicate, force);
    }

    public BulkText(IQ iq, Model model, IRI self, IRI templateMime, IRI newPredicate, boolean force) throws IOException {
        init(iq, model, self, templateMime, newPredicate, force);
    }

    public void init(IQ iq, Model model, IRI self, IRI templateMime, IRI newPredicate, boolean force) throws IOException {
        super.init(iq, model, self);
        this.templateMime = templateMime==null?iq.toIRI(COMMONS.MIME_HBS):templateMime;
        this.newPredicate = newPredicate;
        this.model = model == null ? new DynamicModelFactory().createEmptyModel(): model;
        this.force = force;
    }

    /**
     * Combine query results with a template
     *
     * @param query         query source of models
     * @param template      template for each model
     * @param predicate     property for new triples
     * @return
     */
    public Set<IRI> perform(IRI query, Resource template, IRI predicate) throws IOException {
        Set<IRI> done = new HashSet<>();
        List<Map<String, Object>> results = executeQuery(query);
        log.info("perform.results: {}", results.size());
        if (results.isEmpty()) return done;

        String hbs = library.getContent(template, templateMime);
        if (hbs==null) throw new IOException("missing template: "+template);
        String hashed = Fingerprint.identify(query.stringValue() + "&" + template.stringValue());

        IRI contextIRI = iq.toIRI("urn:" + hashed);
        String urn = "urn:" + hashed + "#";
        log.info("perform.results.urn: {}", urn);

        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> result = results.get(i);
            IRI newIRI = iq.toIRI(urn + i);
            result.put("@id", newIRI);
            log.info("perform: {} -& {} ==> {}", query, template, predicate);
            boolean exists = model.getStatements(newIRI, predicate, contextIRI).iterator().hasNext();
            log.info("perform.exists: {} => {} & {}", newIRI, exists, force);
            if (!exists || force) {
                render(newIRI, predicate, hbs, result, model);
                done.add(newIRI);
            }
        }
        return done;
    }

    protected void render(IRI newIRI, IRI predicate, String hbs, Map<String, Object> result, Model model) throws IOException {
        String blended = HBSRenderer.template(hbs, result);
        Literal literal = vf.createLiteral(blended, templateMime);
        model.remove(newIRI, predicate,null,newIRI);
        model.add(newIRI, predicate, literal, newIRI);
    }

    @Override
    @RDF(COMMONS.IQ_NS+"bulk-text")
    public Set<IRI> execute(IRI subject, Resource object) {
        try {
            return perform(subject, object, newPredicate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
