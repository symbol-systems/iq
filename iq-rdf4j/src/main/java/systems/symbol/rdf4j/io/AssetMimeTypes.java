package systems.symbol.rdf4j.io;

import systems.symbol.ns.COMMONS;
import org.eclipse.rdf4j.model.IRI;

import java.util.HashMap;

public class AssetMimeTypes extends HashMap<String,String> {
    
    public AssetMimeTypes() {
        put("asq",	COMMONS.MIME_TYPE+ "application/x-asq");
        put("sparql", COMMONS.MIME_SPARQL);
        put("graphql", COMMONS.MIME_GRAPHQL);
        put("graphqls", COMMONS.MIME_GRAPHQL);
        put("hbs", COMMONS.MIME_HBS);
        put("html", COMMONS.MIME_HTML);
        put("xhtml", COMMONS.MIME_TYPE+ "application/xhtml+xml");
        put("txt", COMMONS.MIME_PLAIN);
        put("json", COMMONS.MIME_JSON);
        put("css", COMMONS.MIME_TYPE+"plain/css");
        put("xml", COMMONS.MIME_TYPE+"application/xml");
    }

    public static String toMimeType(IRI mimeIRI) {
        if (!mimeIRI.stringValue().startsWith(COMMONS.MIME_TYPE)) return null;
        return mimeIRI.stringValue().substring(COMMONS.MIME_TYPE.length());
    }

    public String toMimeType(String name) {
        int ix = name.lastIndexOf(".");
        if (ix >= 0) {
            String extn = name.substring(ix+1);
//            System.out.println("getMimeType: "+name +" -> "+ extn);
            return get(extn);
        }
        return get(name);
    }}
