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

import java.io.IOException;
import java.util.*;

public abstract class RenderAbstract extends IQIntent {

    IRI templateMime = null;
    protected RenderAbstract() {}

    public void init(IQ iq, Model model, IRI self, IRI templateMime) throws IOException {
        super.init(iq, model, self);
        this.templateMime = templateMime;
    }

    /**
     * Bind query results as data and render a single template into a new Literal
     *
     * @param query         query source of models
     * @param template      template for each model
     * @return Set of one IRI for the new triple
     */
    public Set<IRI> perform(IRI query, Resource template) throws IOException {
        Set<IRI> done = new HashSet<>();
        List<Map<String, Object>> results = executeQuery(query);
        log.info("perform.results: {}", results.size());

        String hbs = library.getContent(template, templateMime);
        String hashed = Fingerprint.identify(query.stringValue() + "&" + template.stringValue());
        IRI newIRI = iq.toIRI("urn:" + hashed);

        Map<String,Object> ctx = new HashMap<>();
        ctx.put("self", newIRI);
        ctx.put("data", results);
        log.info("perform: {} -& {}", query, template);

        String blended = HBSRenderer.template(hbs, ctx);
//        log.info("perform.blended: {} ==>\n{}", newIRI.stringValue(), blended);
        process(newIRI, blended);
        done.add(newIRI);
        return done;
    }

    abstract protected void process(IRI entity, String blended) throws IOException;

    @Override
    @RDF(COMMONS.IQ_NS+"blend-text")
    abstract public Set<IRI> execute(IRI subject, Resource object);

}
