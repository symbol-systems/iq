package systems.symbol.mcp.resource;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.mcp.I_MCPResource;
import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;

import java.util.Set;

/**
 * NamespacesResourceProvider — serves registered RDF prefix mappings.
 *
 * <p>URI: {@code iq://self/namespaces}
 *
 * <p>Returns the registered namespace prefixes from the RDF4J repository so
 * the LLM can write valid prefixed SPARQL without guessing URIs.
 * Content is JSON-LD context or plain JSON mapping prefix → namespace.
 */
public class NamespacesResourceProvider implements I_MCPResource {

    private static final Logger log = LoggerFactory.getLogger(NamespacesResourceProvider.class);
    private static final String URI = "iq://self/namespaces";

    private final Repository repository;

    public NamespacesResourceProvider(Repository repository) { this.repository = repository; }

    @Override public String getUri()         { return URI; }
    @Override public String getLabel()       { return "Namespace Prefixes"; }
    @Override public String getMimeType()    { return "application/json"; }
    @Override
    public String getDescription() {
        return "All registered RDF namespace prefixes for this IQ instance. " +
               "Include these PREFIX declarations at the top of every SPARQL query.";
    }

    @Override
    public I_MCPResult read(MCPCallContext ctx, String uri) throws MCPException {
        log.debug("[NamespacesResource] loading prefixes [trace={}]", ctx.traceId());
        try (RepositoryConnection conn = repository.getConnection()) {
            Set<Namespace> namespaces = conn.getNamespaces().stream()
                    .collect(java.util.stream.Collectors.toSet());

            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            // Also add default IQ prefixes that may not be in the repo's namespace registry
            addDefault(json, "iq",   "iq:", first);       first = false;
            addDefault(json, "owl",  "http://www.w3.org/2002/07/owl#", first);
            addDefault(json, "rdfs", "http://www.w3.org/2000/01/rdf-schema#", first);
            addDefault(json, "rdf",  "http://www.w3.org/1999/02/22-rdf-syntax-ns#", first);
            addDefault(json, "xsd",  "http://www.w3.org/2001/XMLSchema#", first);
            addDefault(json, "sh",   "http://www.w3.org/ns/shacl#", first);
            addDefault(json, "mcp",  "urn:mcp:", first);

            for (Namespace ns : namespaces) {
                json.append(",\"").append(escape(ns.getPrefix())).append("\":\"")
                    .append(escape(ns.getName())).append('"');
            }
            json.append('}');
            return MCPResult.okJson(json.toString());
        } catch (Exception ex) {
            throw MCPException.internal("Failed to load namespaces: " + ex.getMessage(), ex);
        }
    }

    private static void addDefault(StringBuilder sb, String prefix, String ns, boolean first) {
        if (!first) sb.append(',');
        sb.append('"').append(prefix).append("\":\"").append(ns).append('"');
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
