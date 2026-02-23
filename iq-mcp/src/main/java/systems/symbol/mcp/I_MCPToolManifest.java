package systems.symbol.mcp;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.IRI;

/**
 * MCP Tool Manifest interface defining a tool's identity, schema, and governance.
 * 
 * Tool manifests are typically stored as RDF triples in the realm's fact graph.
 * They describe:
 * - name, description, category
 * - input schema (SHACL shapes)
 * - output schema
 * - authorization/trust checks
 * - rate limits and quotas
 */
public interface I_MCPToolManifest {

    /**
     * Retrieves the adapter's self identity (IRI).
     * 
     * @return the IRI of this adapter
     */
    IRI getSelf();

    /**
     * Retrieves the tool's unique name.
     * 
     * @return tool name (e.g., "sparql.query")
     */
    String getName();

    /**
     * Retrieves the tool's description.
     * 
     * @return human-readable description
     */
    String getDescription();


    /**
     * Retrieves the SHACL shape constraining input parameters.
     * 
     * @return RDF Model containing SHACL PropertyShape
     */
    Model getInputShape();

    /**
     * Retrieves the SHACL shape constraining output format.
     * 
     * @return RDF Model containing SHACL PropertyShape
     */
    Model getOutputShape();

    /**
     * Retrieves the SPARQL ASK query used for authorization checks.
     * 
     * @return SPARQL ASK query text (or null if no authorization required)
     */
    String getAuthorizationQuery();

    /**
     * Retrieves the rate limit (requests per minute).
     * 
     * @return rate limit as int, or -1 for unlimited
     */
    int getRateLimit();

    /**
     * Retrieves the estimated cost/quota consumption for one invocation.
     * 
     * @return cost units (e.g., tokens for LLM tools)
     */
    int getCost();
}
