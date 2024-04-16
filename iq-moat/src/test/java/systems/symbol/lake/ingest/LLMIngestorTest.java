package systems.symbol.lake.ingest;

import org.apache.commons.vfs2.FileSystemException;
import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.llm.openai.ChatGPT;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

class LLMIngestorTest {

String llmPrompt = "as an expert ontologist use SKOS and DC vocabularies to describe this text using RDF Turtle: ";
@Test
void testIngest() throws RepositoryException, FileSystemException {
File from = new File("./tested/tika/");

String openaiApiKey = System.getenv("OPENAI_API_KEY");
if (openaiApiKey != null) {
try {
ChatGPT ai = new ChatGPT(openaiApiKey, 1000);

boolean[] done = {false};
LLMIngestor ingest = new LLMIngestor(ai, llmPrompt, content -> {
done[0] = true;
System.out.println("test.ingest.LLM: " + content.getSelf() + " ==> " + content.getContent());
});
VFSCrawler crawler = new VFSCrawler(new FileContentConverter(new XHTMLChunkIngestor(new ThrottleIngestor<>(1, ingest))));
crawler.crawl(from.toURI());
assert done[0];
} catch (RuntimeException e) {
System.out.println("test.error: " + e.getMessage());
}
}
}
}
