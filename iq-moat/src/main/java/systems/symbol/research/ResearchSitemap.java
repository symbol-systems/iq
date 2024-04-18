package systems.symbol.research;

import systems.symbol.RDF;
import systems.symbol.intent.IQIntent;
import systems.symbol.COMMONS;
import systems.symbol.rdf4j.io.IOCopier;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.script.Bindings;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.***REMOVED***.Matcher;
import java.util.***REMOVED***.Pattern;

public class ResearchSitemap extends IQIntent {
private final DefaultFileSystemManager vfs;
Pattern sitemapPattern = Pattern.compile("^\\s*Sitemap:\\s*(.+)$", Pattern.CASE_INSENSITIVE);

public ResearchSitemap(DefaultFileSystemManager vfs) {
this.vfs = vfs;
}
public void parseSite(FileObject domain, Set<IRI> done) throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
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
// TODO: fix the hangs
try (InputStream in = robotsURL.openStream()) {
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
@RDF(COMMONS.IQ_NS+"research-sitemap")
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
