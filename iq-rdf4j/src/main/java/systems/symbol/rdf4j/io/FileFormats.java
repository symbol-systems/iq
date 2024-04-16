package systems.symbol.rdf4j.io;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileFormats {
    private static final Logger log = LoggerFactory.getLogger(FileFormats.class);

    static Map<String, RDFFormat> rdfFormats = new HashMap<>();
    static Map<String, Object> txtFormats = new HashMap<>();
    static Map<String, Object> scriptFormats = new HashMap<>();

    static {
        // common RDF file extensions
        rdfFormats.put("rdf", RDFFormat.RDFXML);
        rdfFormats.put("owl", RDFFormat.RDFXML);
        rdfFormats.put("ttl", RDFFormat.TURTLE);
        rdfFormats.put("nt", RDFFormat.NTRIPLES);
        rdfFormats.put("nq", RDFFormat.NQUADS);
        rdfFormats.put("jsonld", RDFFormat.JSONLD);
        rdfFormats.put("trig", RDFFormat.TRIG);
        rdfFormats.put("json", RDFFormat.JSONLD);

        // common 'text' file extensions
        txtFormats.put("rq", "application/sparql-query");
        txtFormats.put("txt", "text/plain");
        txtFormats.put("md", "text/markdown");
        txtFormats.put("hbs", "text/x-handlebars");
        txtFormats.put("asq",	"application/x-asq");
        txtFormats.put("sparql", "application/x-sparql-query");
        txtFormats.put("graphql", "text/graphql");
        txtFormats.put("graphqls", "text/graphql");
        txtFormats.put("html", "text/html");
        txtFormats.put("xhtml", "application/xhtml+xml");
        txtFormats.put("json", "application/json");
        txtFormats.put("yaml", "application/yaml");
        txtFormats.put("css", "plain/css");
        txtFormats.put("xml", "application/xml");

        // identify supported scripts
        ScriptEngineManager sem = new ScriptEngineManager();
        for (ScriptEngineFactory factory : sem.getEngineFactories()) {
            log.info("script.support: " + factory.getLanguageName() + " v" + factory.getLanguageVersion());
            for (String ext : factory.getExtensions()) {
                // log.info("script.ext: " + ext);
                // prefer text/ mime types
                for (String mime : factory.getMimeTypes()) {
                    log.debug("script.mime: " + mime);
                    if (!scriptFormats.containsKey(ext) && mime.startsWith("text/")) {
                        scriptFormats.put(ext, mime);
                        log.debug("script.format: " + ext + " --> " + mime);
                    }
                }
                // ensure we have a mimetype, even if not text/
                if (!scriptFormats.containsKey(ext)) {
                    String mime = factory.getMimeTypes().get(0);
                    scriptFormats.put(ext, mime);
                    log.info("script.default: "+ext+" -> "+mime);
                }
            }
        }
    }

    protected static String findMatchingMime(Map<String, Object> formats, String filename) {
        // Extract the file extension from the filename
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            String fileExtension = filename.substring(lastDotIndex + 1).toLowerCase();

            // Check if the extension exists & return format
            if (formats.containsKey(fileExtension)) {
                return formats.getOrDefault(fileExtension, "").toString();
            }
        }
        return null;
    }

    public static String toPlainMime(String filename) {
        String format = findMatchingMime(scriptFormats, filename);
        if (format != null) return format;
        return findMatchingMime(txtFormats, filename);
    }

    public static IRI toMime(String path) {
        String supportedMimetype = toPlainMime(path);
        if (supportedMimetype == null) return null;
        return Values.iri("urn:"+supportedMimetype);
    }

    public static RDFFormat toRDFFormat(String filename) {
        // Extract the file extension from the filename
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            String fileExtension = filename.substring(lastDotIndex + 1).toLowerCase();

            // Check if the extension exists & return format
            if (rdfFormats.containsKey(fileExtension)) {
                return rdfFormats.get(fileExtension);
            }
        }
        return null;
    }

}
