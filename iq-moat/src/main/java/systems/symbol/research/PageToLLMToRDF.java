package systems.symbol.research;

import systems.symbol.annotation.RDF;
import systems.symbol.intent.AbstractIntent;
import systems.symbol.intent.IQIntent;
import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.ingest.*;
import systems.symbol.llm.I_LLM;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.iq.IQ;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static systems.symbol.platform.Provenance.generated;

public class PageToLLMToRDF extends IQIntent {

//protected VFSCrawler crawler;
protected Consumer<ContentEntity<String>> processor;
protected Consumer<ContentEntity<String>> ingestRDF;
protected Consumer<ContentEntity<String>>  llm2RDF;
public PageToLLMToRDF(IQ iq, Model model, IRI self, I_LLM<String> llm, String prompt) throws IOException {
init(iq, model, self, llm, prompt, RDFFormat.JSONLD);
}
public void init(IQ iq, Model model, IRI self, I_LLM<String> llm, String prompt, RDFFormat format) throws IOException {
super.init(iq, model, self);
ingestRDF = new RDFModelIngestor(model, format);
llm2RDF = new LLMIngestor(llm, "Reply only using valid "+ format.getName()+" RDF: "+prompt, ingestRDF);
processor = new WebpageIngestor(new XHTMLChunkIngestor(llm2RDF));
}
@Override
@RDF(COMMONS.IQ_NS+"research-page")
public Set<IRI> execute(IRI agent, Resource feed) {
Set<IRI> done = new HashSet<>();
try {
generated(model, agent, SELF, feed, getIdentity());
processor.accept(new ContentEntity<String>((IRI)feed, null));
} catch (Exception e) {
throw new RuntimeException(e);
}
return done;
}
}
