package systems.symbol.research;

import systems.symbol.intent.Performer;
import systems.symbol.llm.openai.ChatGPT;
import systems.symbol.ns.COMMONS;
import systems.symbol.rdf4j.io.RDFDump;
import systems.symbol.rdf4j.iq.IQConnection;
import systems.symbol.rdf4j.iq.RepoModel;
import systems.symbol.rdf4j.store.LocalAssetRepository;
import systems.symbol.rdf4j.util.RDFHelper;
import systems.symbol.rdf4j.util.RDFPrefixer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Set;

import static org.eclipse.rdf4j.rio.RDFFormat.*;

class PageToLLMToRDFTest {

File ttl = new File("src/test/resources/assets/agent.ttl");
File moat = new File("tested/moat/");
ValueFactory vf = SimpleValueFactory.getInstance();
IRI self = vf.createIRI(COMMONS.IQ_NS_TEST);

@Test
void testCrawlPage() throws Exception {
String openaiApiKey = System.getenv("OPENAI_API_KEY");
if (openaiApiKey != null) {
ChatGPT ai = new ChatGPT(openaiApiKey, 1000);
LocalAssetRepository repo = new LocalAssetRepository();
IRI loaded = repo.load(ttl, self);
System.out.println("page2rdf.page: " + loaded);

IQConnection iq = new IQConnection(self, repo.getConnection());
Model model = new RepoModel(iq.getConnection());
model.setNamespace("my", "https://:systems.symbol/v0/my#");
String namespaces = RDFPrefixer.toTurtle(model.getNamespaces());

String prompt = "Extract the key topics as SKOS concepts such as my:TopicExample a skos:Concept:\n"+namespaces;
PageToLLMToRDF page2rdf = new PageToLLMToRDF(iq, model, self, ai, prompt);

Performer performer = new Performer(model, self, page2rdf);

int before_size = model.size();
Set<IRI> added = performer.perform();
int after_size = model.size();
RDFDump.dump(model, Files.newOutputStream(new File(moat, "page2llm2rdf.ttl").toPath()), TURTLE);
System.out.println("page2rdf.page.done: " + before_size + " ++ " + added.size() + " -> " + after_size);
//assert !added.isEmpty();
assert after_size > before_size;
assert after_size > before_size + added.size();
}
}


}