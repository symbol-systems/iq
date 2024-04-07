package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.apache.commons.vfs2.FileObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class XHTMLChunkIngestor extends AbstractIngestor<ContentEntity<String>> {
protected int CHUNK_SIZE = 512;
protected XHTMLChunkIngestor() {}
public XHTMLChunkIngestor(Consumer<ContentEntity<String>> processor) {
super(processor);
}

protected void processXHTML(ContentEntity<String> page) throws IOException, URISyntaxException {
Document doc = Jsoup.parse(page.getContent().toString(), page.getIdentity().stringValue());
StringBuilder sb = new StringBuilder();

Elements pages = doc.select(".page");

// chunk by page or p-tag
if (!pages.isEmpty()) {
processChunks(page, sb, pages);
} else {
Elements paras = doc.select("p");
processChunks(page, sb, paras);
}
}

protected void processChunks(ContentEntity<String> file, StringBuilder sb, Elements sections) throws IOException {
log.info("xhtml.chunks: {} => {}", file.getIdentity(), sections.size());
int chunk = 0;
for (Element element : sections) {
processChunk(file, sb, element, chunk++);
}
// final chunk, less than chunk-size
processTextChunk(file,sb,chunk);
}

protected void processChunk(ContentEntity<String> file, StringBuilder sb, Element element, int chunk) throws IOException {
String content = element.text().trim();
log.info("xhtml.chunk: {} => {} -> {}", file.getIdentity(), chunk, content);
if (content.isEmpty()) return;
sb.append(content);
if (sb.length() >= CHUNK_SIZE) {
processTextChunk(file, sb, chunk);
}
}

protected void processTextChunk(ContentEntity<String> file, StringBuilder sb, int chunk) {
log.debug("xhtml.text: {} --> {}" ,chunk, sb);
if (sb.length()<1) return;
String content = sb.substring(0, Math.min(CHUNK_SIZE, sb.length()));
ContentEntity<String> entity = new ContentEntity<>(file.getIdentity()+"#chunk_"+chunk, content);
sb.setLength(Math.max(sb.length()-CHUNK_SIZE,0));
next(entity);
}

@Override
public void accept(ContentEntity page) {
try {
processXHTML(page);
} catch (IOException | URISyntaxException e) {
throw new RuntimeException(e);
}
}
}
