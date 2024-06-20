package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.string.PrettyString;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RSSIngestor extends AbstractIngestor<ContentEntity<String>> {
private final Model model;
private final IRI clzzIRI = Values.iri("urn:" + getClass().getCanonicalName());
Consumer<ContentEntity<String>> next;

public RSSIngestor(Model model) {
this.model = model;
}

public RSSIngestor(Model model, Consumer<ContentEntity<String>> next) {
this.model = model;
this.next = next;
}

@Override
public void accept(ContentEntity<String> page) {
try {
parseRSS(page);
} catch (Exception e) {
log.error("rss.failed: {}", page.getSelf(), e);
}
}

public void parseRSS(ContentEntity<String> page) throws Exception {
DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();
String content = page.getContent().toString();
Document doc = builder.parse(new InputSource(new StringReader(content)));
doc.getDocumentElement().normalize();

parseRSS(page.getSelf(), doc);
}

private void parseRSS(IRI page, Document doc) {
log.info("rss.page: {}", page);

// Process channel information
NodeList channelNodes = doc.getElementsByTagName("channel");
for (int i = 0; i < channelNodes.getLength(); i++) {
Node channelNode = channelNodes.item(i);
if (channelNode.getNodeType() == Node.ELEMENT_NODE) {
Element channelElement = (Element) channelNode;
IRI channelLink = parseChannel(channelElement);
parseRSSArticles(channelLink, channelElement);
}
}
}

private void parseRSSArticles(IRI channelLink, Element channelElement) {
// Process items within the channel
NodeList itemNodes = channelElement.getElementsByTagName("item");
for (int j = 0; j < itemNodes.getLength(); j++) {
Node itemNode = itemNodes.item(j);
if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
Element itemElement = (Element) itemNode;
IRI itemLink = parseRSSArticle(channelLink, itemElement);
addCategories(channelLink, itemElement, itemLink);
}
}
}

private IRI parseChannel(Element channelElement) {
String title = getElementText(channelElement, "title");
String link = getElementText(channelElement, "link");
String description = getElementText(channelElement, "description");
String lastBuildDate = getElementText(channelElement, "lastBuildDate");
log.info("rss.channel: {} --> {}", link, title);

IRI channel = Values.iri(link);
add(channel, toDCPredicate("title"), title, channel);
add(channel, toDCPredicate("description"), description, channel);
add(channel, toDCPredicate("link"), link, channel);
add(channel, toDCPredicate("created"), lastBuildDate, channel);

return channel;
}

private IRI parseRSSArticle(IRI channel, Element itemElement) {
String title = getElementText(itemElement, "title");
String description = getElementText(itemElement, "description");
String link = getElementText(itemElement, "link");
log.info("rss.item: {} -> {} --> {}", link, title, description);

IRI itemIRI = Values.iri(link);
model.add(itemIRI, RDF.TYPE, clzzIRI, channel);
add(itemIRI, toDCPredicate("title"), title, channel);
add(itemIRI, toDCPredicate("description"), description, channel);
add(itemIRI, toDCPredicate("link"), link, channel);
if (next !=null) next.accept( new ContentEntity<String>(itemIRI, title+"\n"+description, "text/plain"));
return itemIRI;
}

private String getElementText(Element element, String tagName) {
NodeList nodeList = element.getElementsByTagName(tagName);
if (nodeList.getLength() > 0) {
return nodeList.item(0).getTextContent().trim();
}
return "";
}

private List<String> getCategories(Element item) {
List<String> categories = new ArrayList<>();

NodeList categoryNodes = item.getElementsByTagName("category");
for (int i = 0; i < categoryNodes.getLength(); i++) {
Node categoryNode = categoryNodes.item(i);
if (categoryNode.getNodeType() == Node.ELEMENT_NODE) {
Element categoryElement = (Element) categoryNode;
String category = categoryElement.getTextContent().trim();
categories.add(category);
}
}
return categories;
}

private void addCategories(IRI channelLink, Element item, IRI itemLink) {
String schemaIRI = channelLink.stringValue()+"#";
List<String> categories = getCategories(item);
for (String category : categories) {
String id = PrettyString.toPascalCase(category.trim());
IRI conceptIRI = Values.iri(schemaIRI,id);
model.add(itemLink, RDF.TYPE, conceptIRI, channelLink);

if (model.contains(conceptIRI,RDF.TYPE, SKOS.CONCEPT)) return;

model.add(conceptIRI, RDF.TYPE, SKOS.CONCEPT, channelLink);
model.add(conceptIRI, SKOS.PREF_LABEL, Values.***REMOVED***(category), channelLink);
model.add(conceptIRI, SKOS.IN_SCHEME, Values.iri(schemaIRI), channelLink);
}

}
private void add(Resource subject, IRI predicate, String object, IRI channel) {
if (object==null) return;
model.add(subject, predicate, Values.***REMOVED***(object), channel);
}

private IRI toRSSPredicate(String p) {
return Values.iri("http://purl.org/rss/1.0/", p);
}
private IRI toDCPredicate(String predicate) {
return Values.iri("http://purl.org/dc/elements/1.1/" + predicate);
}
}
