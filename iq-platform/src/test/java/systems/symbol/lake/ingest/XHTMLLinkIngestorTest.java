package systems.symbol.lake.ingest;

import systems.symbol.lake.crawl.VFSCrawler;
import systems.symbol.tools.Online;

import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.jupiter.api.Test;

import com.squareup.okhttp.internal.framed.Ping;

import java.net.URI;
import java.net.URISyntaxException;

class XHTMLLinkIngestorTest {

@Test
void processXHTMLLinks() throws FileSystemException, URISyntaxException {
String site = "https://arxiv.org";
String from = site + "/list/cs.AI/pastweek?skip=0&show=5";

if (!Online.isPing(site))
return;
boolean done[] = { false };
Model model = new LinkedHashModel();
XHTMLLinkIngestor ingestHTML = new XHTMLLinkIngestor(model, new URI(site), t -> {
// System.out.println("links.found: " + t.getIdentity());
done[0] = t.getSelf().stringValue().startsWith(site);
});
VFSCrawler crawler = new VFSCrawler(ingestHTML);
System.out.println("links.crawl: " + from);
assert !done[0];
crawler.crawl(new URI(from));
assert done[0];
}
}