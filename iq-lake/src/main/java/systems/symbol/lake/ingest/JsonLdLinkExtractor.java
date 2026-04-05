package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.eclipse.rdf4j.model.IRI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class JsonLdLinkExtractor implements Consumer<ContentEntity<String>> {
private static final Logger log = LoggerFactory.getLogger(JsonLdLinkExtractor.class);
private final FileSystemManager vfs;
private final Consumer<ContentEntity<String>> next;
Set<IRI> seen = new HashSet<>();
String contentType = "application/ld+json";

public JsonLdLinkExtractor(FileSystemManager vfs, Consumer<ContentEntity<String>> next) throws FileSystemException {
this.next = next;
this.vfs = vfs;
}

@Override
public void accept(ContentEntity<String> html) {
try {
log.info("accept: {}", html.getSelf());
ingestJSONLD(html);
} catch (IOException e) {
log.error("json.parse.failed: {}", html.getSelf(), e);
throw new RuntimeException(e);
}
}

IRI next(ContentEntity<String> jsonld) {
if (next != null) {
next.accept(jsonld);
return jsonld.getSelf();
}
return null;
}

public void ingestJSONLD(ContentEntity<String> source) throws IOException {
String content = source.getContent();
ContentEntity<String> entity = new ContentEntity<>(source.getSelf(), content, contentType);
log.info("json.parsed: {} x {}", entity.getSelf(), content.length());
extractJSON(entity.getSelf(), content);
}

private void extractJSON(IRI page, String html) {
if (seen.contains(page))
return;

Document document = Jsoup.parse(html);
seen.add(page);
Elements jsonLdScripts = document.select("[type=" + contentType + "]");
log.info("json.scripts.found: {}", jsonLdScripts);
jsonLdScripts.forEach(script -> {
String src = script.attr("src");
if (!src.isEmpty()) {
try {
String jsonURL = toAbsoluteURL(page.stringValue(), src);
log.info("json.script.url: {} + {} ==> {}", page.stringValue(), src, jsonURL);
if (jsonURL != null) {
FileObject json = this.vfs.resolveFile(jsonURL);
seen.add(next(new ContentEntity<String>(jsonURL, json.getContent().getString("UTF-8"))));
}
} catch (FileSystemException e) {
log.error("json.script.vfs: {}", src, e);
// throw new RuntimeException(e);
} catch (IOException e) {
log.error("json.script.io: {}", src, e);
// throw new RuntimeException(e);
}
} else {
String json = script.html();
if (!json.isEmpty()) {
log.info("json.script.inline: {}", json);
next(new ContentEntity<String>(page, json, contentType));
}
}
});
}

static public String toAbsoluteURL(String pageURL, String linkURL) {
try {
return new URI(pageURL).resolve(linkURL).toURL().toExternalForm();
} catch (IllegalArgumentException | MalformedURLException | URISyntaxException e) {
log.error("json.url.error: {} + {} ==> {}", pageURL, linkURL, e.getMessage());
return null;
}
}

}
