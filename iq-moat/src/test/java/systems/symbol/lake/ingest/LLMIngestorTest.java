package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.llm.openai.ChatGPT;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

class LLMIngestorTest {

String llmPrompt = "as an expert ontologist use SKOS and DC vocabularies to describe this text using RDF Turtle: ";
@Test
void testIngest() throws RepositoryException, IOException {
File from = new File("./tested/tika/");

String openaiApiKey = System.getenv("OPENAI_API_KEY");
if (openaiApiKey != null) {
ChatGPT ai = new ChatGPT(openaiApiKey, 1000);

boolean done[] = {false};
LLMIngestor ingestor = new LLMIngestor(ai, llmPrompt, content -> {
done[0] = true;
System.out.println("test.ingest.LLM: " + content.getIdentity() + " ==> " + content.getContent());
});
VFSCrawler crawler = new VFSCrawler(new FileContentConverter(new XHTMLChunkIngestor(new ThrottleIngestor<>(1, ingestor))));
crawler.crawl(from.toURI());
assert done[0];
}
}
}
