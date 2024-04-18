package systems.symbol.rdf4j.io;

import systems.symbol.COMMONS;

import java.util.HashMap;

public class AssetMimeTypes {
    public static HashMap<String, String> types = new HashMap<>();

    static  {
        types.put("sparql", COMMONS.MIME_SPARQL);
        types.put("graphql", COMMONS.MIME_GRAPHQL);
        types.put("graphqls", COMMONS.MIME_GRAPHQL);
        types.put("hbs", COMMONS.MIME_HBS);
        types.put("html", COMMONS.MIME_XHTML);
        types.put("txt", COMMONS.MIME_PLAIN);
        types.put("json", COMMONS.MIME_JSON);
        types.put("css", "plain/css");
        types.put("xml", "application/xml");
        types.put("xhtml", "application/xhtml+xml");
    }

//    public static String toMimeType(IRI mimeIRI) {
//        if (!mimeIRI.stringValue().startsWith(COMMONS.MIME_TYPE)) return null;
//        return mimeIRI.stringValue().substring(COMMONS.MIME_TYPE.length());
//    }

    public static String toMimeType(String name) {
        int ix = name.lastIndexOf(".");
        if (ix >= 0) {
            String extn = name.substring(ix+1).toLowerCase();
            System.out.println("toMimeType: "+name +" -> "+ extn);
            return types.get(extn);
        }
        return types.get(name);
    }}
