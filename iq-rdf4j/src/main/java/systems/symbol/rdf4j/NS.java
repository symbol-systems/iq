package systems.symbol.rdf4j;

import systems.symbol.COMMONS;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import systems.symbol.util.URLHelper;

public class NS implements COMMONS {
    public static String KEY_AT_ID = "@id", KEY_AT_THIS = "@this";
    public static String KEY_CONTEXT = "@context", KEY_SELF = "@self";

    Map<String, String> ns2prefix = new HashMap<>();
    Map<String,String> prefix2ns = new HashMap<>();
    String baseNS;

    public NS(String baseNS) {
        this.baseNS = baseNS;
        defaults(this);
        add("", baseNS);
    }

    public static NS defaults() {
        return new NS(IQ_NS_TEST);
    }

    public static NS defaults(NS ns) {
        ns.add(RDF.PREFIX,RDF.NAMESPACE);
        ns.add(RDFS.PREFIX,RDFS.NAMESPACE);
        ns.add(SKOS.PREFIX, SKOS.NAMESPACE);

        ns.add(DCAT.PREFIX,DCAT.NAMESPACE);
        ns.add(DCTERMS.PREFIX,DCTERMS.NAMESPACE);
        ns.add("dct",DCTERMS.NAMESPACE);
//        ns.add("cnt",CNT);
        return ns;
    }

    void add(String prefix, String ns) {
        ns2prefix.put(ns,prefix);
        prefix2ns.put(prefix,ns);
    }

    public String getNS(String prefix) {
        return prefix2ns.get(prefix);
    }

    public String findPrefix(String iri) {
        for(String prefix: prefix2ns.keySet()) {
            if (iri.startsWith( prefix2ns.get(prefix))) {
                return prefix;
            }
        }
        return null;
    }

    public String localize(IRI _iri) {
        String iri = _iri.stringValue();
        String prefix = findPrefix(iri);
        if (prefix==null) return null;
        return prefix+":"+iri.substring(prefix2ns.get(prefix).length());
    }

    public String localize(String iri) {
        String prefix = findPrefix(iri);
        String ns = prefix2ns.get(prefix);
        if (ns==null) return null;
        return (prefix.length()==0?"":prefix+":")+iri.substring(ns.length());
    }

    public String globalize(String local) {
        if (baseNS.startsWith(local) && local.length()<baseNS.length()) {
            return null; // can't be more generic than ourselves
        }
        if (local.startsWith(baseNS)) {
            return local;
        }
        if (URLHelper.isValidUrl(local)) {
            return local; // return existing URLs
        }

        int prefix_i = local.indexOf(":");
//System.out.println("globalize.prefix: "+local+" -> "+prefix_i);

        // we may be "prefix:name" or a global IRI
        if (prefix_i>=0) {
            String prefix = local.substring(0,prefix_i);
            local = local.substring(prefix_i + 1);
            String baseNS = getNS(prefix);
            if (baseNS!=null && !baseNS.isEmpty())
                return baseNS + local;
        }
//System.out.println("globalize.local: "+local);
        return baseNS +sanitize(local);
    }

    public static String sanitize(Object org_string) {
        return org_string.toString().trim().replaceAll("[^A-Za-z\\.\\\\/-0-9]+", "-").toLowerCase(Locale.ROOT);
    }

    public boolean contains(String iri) {
        for(String prefix: prefix2ns.keySet()) {
            if (iri.startsWith( prefix2ns.get(prefix))) {
                return true;
            }
        }
        return false;
    }

    public String getBaseNS() {
        return baseNS;
    }
}
