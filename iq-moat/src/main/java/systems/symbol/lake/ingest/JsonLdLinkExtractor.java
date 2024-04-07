package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.eclipse.rdf4j.model.IRI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class JsonLdLinkExtractor implements Consumer<ContentEntity<String>> {
private static final Logger log = LoggerFactory.getLogger(JsonLdLinkExtractor.class);
private final FileSystemManager vfs;
private final Consumer<ContentEntity<String>> next;
Set<IRI> seen = new HashSet<>();

public JsonLdLinkExtractor(Consumer<ContentEntity<String>> next) throws FileSystemException {
this.next = next;
this.vfs = VFS.getManager();
}

@Override
public void accept(ContentEntity html) {
try {
log.info("accept: {}", html.getIdentity());
ingestJSONLD(html);
} catch (IOException e) {
log.error("json.parse.failed: {}", html.getIdentity(), e);
throw new RuntimeException(e);
}
}

IRI next(ContentEntity jsonld) {
if (next != null) {
next.accept(jsonld);
return jsonld.getIdentity();
}
return null;
}

public void ingestJSONLD(ContentEntity<String> source) throws IOException {
String content = source.getContent();
ContentEntity<String> entity = new ContentEntity<>(source.getIdentity(), content);
log.info("json.parsed: {}: {}", entity.getIdentity(), content);
extractJSON(entity.getIdentity(), content);
}

private void extractJSON(IRI page, String html) {
if (seen.contains(page)) return;

Document document = Jsoup.parse(html);
seen.add(page);
// Strategy 1: Extract JSON-LD from script.tags with type 'application/ld+json'
Elements jsonLdScripts = document.select("[type=application/ld+json]");
log.debug("json.scripts.found: {}", jsonLdScripts);
jsonLdScripts.forEach(script -> {
String src = script.attr("src");
if (!src.isEmpty()) {
try {
String jsonURL = toAbsoluteURL(page.stringValue(), src);
log.debug("json.script.url: {}", jsonURL);
FileObject json = this.vfs.resolveFile(jsonURL);
seen.add(next(new ContentEntity<String>(jsonURL, json.getContent().getString("UTF-8"))));
} catch (FileSystemException e) {
log.error("json.script.vfs: {}", src, e);
throw new RuntimeException(e);
} catch (IOException e) {
log.error("json.script.io: {}", src, e);
throw new RuntimeException(e);
}
} else {
String json = script.html();
if (!json.isEmpty()) {
log.info("json.script.inline: {}", json);
next(new ContentEntity<String>(page, json));
}
}
});
}

static public String toAbsoluteURL(String pageURL, String linkURL) {
try {
URL base = new URL(pageURL);
URL absoluteURL = new URL(base, linkURL);
return absoluteURL.toString();
} catch (MalformedURLException e) {
return null;
}
}

}
