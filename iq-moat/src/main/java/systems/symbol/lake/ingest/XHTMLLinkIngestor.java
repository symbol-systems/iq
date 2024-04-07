package systems.symbol.lake.ingest;

import systems.symbol.lake.ContentEntity;
import systems.symbol.string.Validate;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.http.HttpFileProvider;
import org.apache.commons.vfs2.provider.https.HttpsFileProvider;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * Ingestor class for processing XHTML files and extracting links.
 */
public class XHTMLLinkIngestor implements Consumer<FileObject> {
    protected static final Logger log = LoggerFactory.getLogger(XHTMLLinkIngestor.class);
    protected URI self;
    private DefaultFileSystemManager vfs  = new DefaultFileSystemManager();
    protected Consumer<ContentEntity<?>> processor;
    Model model;
    ValueFactory vf = SimpleValueFactory.getInstance();
    IRI clzz = vf.createIRI("urn:"+getClass().getCanonicalName());

    protected XHTMLLinkIngestor() {
    }

    /**
     * Constructor for XHTMLLinkIngestor.
     *
     * @param model     The RDF4J model to store extracted information.
     * @param processor Consumer to process ContentEntity instances.
     */
    public XHTMLLinkIngestor(Model model, URI self, Consumer<ContentEntity<?>> processor) throws FileSystemException {
        this.model = model;
        this.processor = processor;
        this.self = self;
        this.vfs.addProvider("http", new HttpFileProvider());
        this.vfs.addProvider("https", new HttpsFileProvider());
        this.vfs.addProvider("file", new DefaultLocalFileProvider());
        this.vfs.init();
/*
        this.vfs.setFilesCache(new DefaultFilesCache());
        this.vfs.setCacheStrategy(CacheStrategy.ON_RESOLVE);
*/
        this.vfs.setBaseFile(new File(""));
    }

    /**
     * Processes an XHTML file, extracting links and invoking the processor.
     *
     * @param file The XHTML file to process.
     * @throws IOException If an I/O error occurs during processing.
     */
    protected void processXHTML(FileObject file) throws IOException, URISyntaxException {
        log.info("page: " + file.getURI());
        extractLinks(model, file);
//        file.resolveFile("href://"+file.getPublicURIString());
    }

    /**
     * Extracts links from the HTML document and processes each link.
     *
     * @param model The RDF4J model.
     * @param file  The XHTML file.
     * @throws IOException        If an I/O error occurs during extraction.
     * @throws URISyntaxException If a link has a malformed URI.
     */
    private void extractLinks(Model model, FileObject file) throws IOException, URISyntaxException {
        Document doc = Jsoup.parse(file.getContent().getInputStream(), "UTF-8", file.getName().getURI());

        // Extract links from the HTML document
        Elements links = doc.select("a[href]");
        log.info("links.found: {} found", links.size());
        URI host = extractHost(file);
        log.info("loginks.extract: {} -> {}", file.getFileSystem().getRoot(), file.getFileSystem().getRootURI());
        for (Element link : links) {
            try {
                extractLink(model, file, host, link);
            } catch (IOException e) {
                log.error("link.failed: {}", link.attr("href"), e);
            } catch (URISyntaxException e) {
                log.error("link.malformed: {}", link.attr("href"), e);
            }
        }
    }

    /**
     * Extracts a link, resolves it, and processes the ContentEntity.
     *
     * @param model The RDF4J model.
     * @param file
     * @param host  The XHTML host.
     * @param link  The HTML link element.
     * @return The extracted link.
     * @throws IOException        If an I/O error occurs during extraction.
     * @throws URISyntaxException If a link has a malformed URI.
     */
    private String extractLink(Model model, FileObject file, URI host, Element link) throws IOException, URISyntaxException {
        String href = link.attr("href");
//        log.info("link.extract: {} -> {} --> {}", href, host, file.getName().getFriendlyURI(), file.getFileSystem().getRoot());
        boolean hasHost = Validate.hasHost(href);
        if (!hasHost) {
            href = JsonLdLinkExtractor.toAbsoluteURL(self.toASCIIString(), href);
        }
        if (href==null) return null;
        if (!Validate.isSameHost(self.toASCIIString(), href)) {
            log.info("link.offsite: {} != {}", host, href);
            return href;
        }
//        log.info("link.resolve: {} -> {} --> {} ==> {}", href, host, file.getName().getFriendlyURI(), file.getFileSystem().getRootURI());

        try (FileObject page = vfs.resolveFile(href)) {
            if (!isSeen(page.getURI())) {
//                log.info("link.resolved: {}", page.getURI());
                ContentEntity entity = new ContentEntity(page.getURI().toString(), link.text());
                processor.accept(entity);
                seen(page.getURI());
                log.info("link.seen: {} -> {}", entity.getIdentity(), isSeen(page.getURI()));
            }
        }
        return href;
    }

    private boolean isSeen(URI uri) {
        return model.getStatements(vf.createIRI(uri.toString()), RDF.TYPE, clzz).iterator().hasNext();
    }

    private void seen(URI uri) {
        model.add(vf.createIRI(uri.toString()), RDF.TYPE, clzz);
    }

    /**
     * Accepts a FileObject and processes it as an XHTML file.
     *
     * @param page The FileObject to process.
     */
    @Override
    public void accept(FileObject page) {
        String extn = page.getName().getExtension();
        log.info("accept: {} ==> {}", page.getURI(), extn);
        try {
            processXHTML(page);
        } catch (IOException e) {
            log.error("xhtml.failed: {}", page.getURI(), e);
        } catch (URISyntaxException e) {
            log.error("url.failed: {}", page.getURI(), e);
        }
    }

    URI extractHost(FileObject file) throws URISyntaxException {
        String uri = file.getURI().getScheme()+"://"+file.getURI().getHost();
        return new URI(uri);
    }

}
