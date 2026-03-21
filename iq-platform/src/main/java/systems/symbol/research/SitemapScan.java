package systems.symbol.research;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import systems.symbol.RDF;
import systems.symbol.intent.IQIntent;
import systems.symbol.platform.IQ_NS;
import systems.symbol.io.IOCopier;

import javax.script.Bindings;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

public class SitemapScan extends IQIntent {
private final DefaultFileSystemManager vfs;
Pattern sitemapPattern = Pattern.compile("^\\s*Sitemap:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

public SitemapScan(DefaultFileSystemManager vfs) {
this.vfs = vfs;
}

public void parseSite(FileObject domain, Set<IRI> done)
throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
FileObject robotsFile = domain.resolveFile("/robots.txt");
Set<FileObject> sitemaps = parseRobotsTxt(robotsFile);

if (sitemaps.isEmpty()) {
FileObject sitemapsFile = domain.resolveFile("/sitemap.xml");
if (sitemapsFile.exists() && sitemapsFile.isFile()) {
sitemaps.add(sitemapsFile);
}
}
for (FileObject sitemap : sitemaps) {
IRI sitemapIRI = parseSitemap(sitemap);
done.add(sitemapIRI);
}
log.info("sitemap.site.done: {} -> {}", sitemaps.size(), done.size());
}

private Set<FileObject> parseRobotsTxt(FileObject robotsFile) throws IOException, URISyntaxException {
Set<FileObject> sitemaps = new HashSet<>();

URL robotsURL = robotsFile.getURL();
log.info("sitemap.robots: {}", robotsURL.toExternalForm());

// Fixed: Add connection and read timeouts to prevent indefinite hangs
// Retry up to 3 times with exponential backoff for transient failures
int maxRetries = 3;
IOException lastException = null;

for (int attempt = 1; attempt <= maxRetries; attempt++) {
try {
URLConnection connection = robotsURL.openConnection();
// Set connection timeout to 5 seconds and read timeout to 10 seconds
connection.setConnectTimeout(5000);
connection.setReadTimeout(10000);

try (InputStream in = connection.getInputStream()) {
log.info("sitemap.read: {}", robotsFile.getURI());
String content = IOCopier.toString(in);

Matcher matcher = sitemapPattern.matcher(content);
if (matcher.matches()) {
String sitemapURL = matcher.group(1).trim();
FileObject found = vfs.resolveFile(sitemapURL);
log.info("sitemap.robot.url: {} -> {}", found.getURI(), sitemaps.size());
sitemaps.add(found);
}

log.info("sitemap.robots.done: {} -> {}", robotsFile.getURI(), sitemaps.size());
return sitemaps;
}
} catch (IOException e) {
lastException = e;
if (attempt < maxRetries) {
long backoffMs = (long) (1000 * Math.pow(2, attempt - 1));
log.warn("sitemap.robots.retry: attempt {} of {} failed, retrying in {}ms: {}", 
 attempt, maxRetries, backoffMs, e.getMessage());
try {
Thread.sleep(backoffMs);
} catch (InterruptedException ie) {
Thread.currentThread().interrupt();
throw new IOException("Interrupted while retrying robots.txt fetch", ie);
}
} else {
log.error("sitemap.robots.failed: {} after {} attempts: {}", 
  robotsFile.getURI(), maxRetries, e.getMessage());
}
}
}

if (lastException != null) {
throw lastException;
}
return sitemaps;
}

public IRI parseSitemap(FileObject page) throws ParserConfigurationException, IOException, SAXException {
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
log.info("sitemap.parse: {}", page.getURI());
FileContent content = page.getContent();
Document doc = builder.parse(new InputSource(new StringReader(content.getString("utf-8"))));
doc.getDocumentElement().normalize();
return vf.createIRI(page.getPublicURIString());
}

@Override
@RDF(IQ_NS.IQ + "sitemap")
public Set<IRI> execute(IRI actor, Resource state, Bindings bindings) {
Set<IRI> done = new HashSet<>();
try {
FileObject site = vfs.resolveFile(state.stringValue());
log.info("sitemap.site: {}", site.getURI());
parseSite(site, done);
} catch (ParserConfigurationException | IOException | SAXException | URISyntaxException e) {
throw new RuntimeException(e);
}
return done;
}
}
