package systems.symbol.research;

import systems.symbol.annotation.RDF;
import systems.symbol.intent.AbstractIntent;
import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.ingest.*;
import systems.symbol.llm.I_LLM;
import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;

import javax.script.Bindings;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static systems.symbol.platform.Provenance.generated;

public class WebPage extends AbstractIntent {

//    protected VFSCrawler crawler;
    protected Consumer<ContentEntity<String>> processor;
    protected Consumer<ContentEntity<String>> ingestRDF;
    protected Consumer<ContentEntity<String>>  llm2RDF;
    public WebPage(IRI self, Model model) throws IOException {
        super(model,self);
    }

    public WebPage(IRI self, Model model, I_LLM<String> llm, String prompt) throws IOException {
        super(model,self);
        init(llm, prompt, RDFFormat.JSONLD);
    }
    public void init(I_LLM<String> llm, String prompt, RDFFormat format) throws IOException {
        if (llm!=null)
            llm2RDF = new LLMIngestor(llm, "Reply only using valid "+ format.getName()+" RDF: "+prompt, ingestRDF);
        ingestRDF = new RDFModelIngestor(model, format);
        processor = new WebpageIngestor(new XHTMLChunkIngestor(llm2RDF==null?ingestRDF:llm2RDF));
        log.info("init: {} --> {}", self, prompt);
    }
    @Override
    @RDF(COMMONS.IQ_NS+"webpage")
    public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
        Set<IRI> done = new HashSet<>();
        log.info("read: {} @ {}", state, actor);
        try {
            generated(model, actor, getSelf(), state, getSelf());
            processor.accept(new ContentEntity<String>((IRI) state, null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return done;
    }
}
