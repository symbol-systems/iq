package systems.symbol.lake.ingest;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Ingestor class for processing OAI-PMH responses and extracting information for RDF modeling.
 */
public class OAIPMHIngestor implements Consumer<FileObject> {
    protected static final Logger log = LoggerFactory.getLogger(OAIPMHIngestor.class);
    Model model;
    ValueFactory vf = SimpleValueFactory.getInstance();
    IRI clzz = vf.createIRI("class:" + getClass().getCanonicalName());

    protected OAIPMHIngestor() {
    }

    /**
     * Constructor for OAIPMHIngestor.
     *
     * @param model     The RDF4J model to store extracted information.
     */
    public OAIPMHIngestor(Model model) {
        this.model = model;
        this.model.setNamespace(DC.NS);
    }

    private void processRecords(Model model, FileObject page) throws IOException, URISyntaxException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(page.getContent().getInputStream());

            // structure is <OAI-PMH> <ListRecords> <record>
            NodeList recordNodes = document.getElementsByTagName("record");

            for (int i = 0; i < recordNodes.getLength(); i++) {
                Node recordNode = recordNodes.item(i);

                if (recordNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element recordElement = (Element) recordNode;
                    processRecord(model, recordElement);
                }
            }
        } catch (Exception e) {
            log.error("failed: {}", page.getURI(), e);
        }
    }

    private void processRecord(Model model, Element record) {
        String identifier = record.getElementsByTagName("identifier").item(0).getTextContent();
        String datestamp = record.getElementsByTagName("datestamp").item(0).getTextContent();

        // Get setSpec information
        NodeList setSpecNodes = record.getElementsByTagName("setSpec");
        for (int i = 0; i < setSpecNodes.getLength(); i++) {
            String setSpec = setSpecNodes.item(i).getTextContent();
            log.info("setSpec: {} -> {}", identifier, setSpec);
            // You can add setSpec information to the RDF model as needed
        }

        IRI recordIRI = vf.createIRI(identifier);
//        log.info("recordIRI: {} -> {}", identifier, recordIRI);
        model.add(recordIRI, RDF.TYPE, clzz);
        model.add(recordIRI, DC.DATE, vf.createLiteral(datestamp));

        NodeList metadata = record.getElementsByTagName("oai_dc:dc");
//        log.info("oai_dc:dc: {} -> {}", recordIRI, metadata.getLength());
        for (int i = 0; i < metadata.getLength(); i++) {
            Node metaNode = metadata.item(i);
            NodeList recordNodes = metaNode.getChildNodes();
            for (int x = 0; x < recordNodes.getLength(); x++) {
                processMetadata(recordIRI, recordNodes.item(x));
            }
        }
    }

    private void processMetadata(IRI recordIRI, Node childNode) {
        String nodeName = childNode.getNodeName();
        int ix = nodeName.indexOf(":");
//        log.info("childNode: {} -> {}", recordIRI, nodeName);
        if (childNode.getNodeType() == Node.ELEMENT_NODE && ix>1) {
            String prefix = nodeName.substring(0,ix);
            Optional<Namespace> namespace = model.getNamespace(prefix);
            if (namespace.isPresent()) {
                String localName = nodeName.substring(ix+1);
                String textContent = childNode.getTextContent().trim();
                log.info("record.tag: {} -> {} ==> {}", namespace.get(), localName, textContent);
                IRI fieldIRI = vf.createIRI(namespace.get().getName(), localName);
                model.add(recordIRI, fieldIRI, vf.createLiteral(textContent));
            }
        } else {
            NodeList recordNodes = childNode.getChildNodes();
            log.info("recurse: {} -> {}", recordIRI, recordNodes.getLength());
            for (int x = 0; x < recordNodes.getLength(); x++) {
                processMetadata(recordIRI, recordNodes.item(x));
            }

        }
    }

    /**
     * Accepts a FileObject and process as an OAI-PMH response.
     *
     * @param page The FileObject to process.
     */
    @Override
    public void accept(FileObject page) {
        String extn = page.getName().getExtension();
        log.info("accept: {} ==> {}", page.getURI(), extn);
        try {
            processRecords(model, page);
        } catch (IOException | URISyntaxException e) {
            log.error("oaipmh.failed: {}", page.getURI(), e);
        }
    }
}
