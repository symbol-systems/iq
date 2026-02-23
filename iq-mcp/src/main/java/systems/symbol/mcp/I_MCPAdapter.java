package systems.symbol.mcp;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import systems.symbol.realm.I_Realm;
import systems.symbol.secrets.SecretsException;

/**
 * MCP Adapter interface for exposing IQ components as Model Context Protocol tools.
 * 
 * Adapters bridge IQ's RDF-first architecture with MCP's tool-based client-server model.
 * Each adapter:
 * 1) Resolves tool manifests and input schemas (stored as RDF).
 * 2) Validates/authorizes requests via SPARQL ASK queries (trust checks).
 * 3) Executes the underlying IQ operation (SPARQL, scripts, agents, LLMs).
 * 4) Audits and returns results, enforcing rate limits and cost tracking.
 */
public interface I_MCPAdapter {

    /**
     * Retrieves the adapter's self identity (IRI).
     * 
     * @return the IRI of this adapter
     */
    IRI getSelf();

    /**
     * Retrieves the realm this adapter operates in.
     * 
     * @return the associated I_Realm
     */
    I_Realm getRealm();

    /**
     * Invokes an MCP tool by name with input parameters.
     * 
     * @param toolName the name of the tool (e.g., "sparql.query", "actor.execute")
     * @param inputModel the RDF model containing tool inputs
     * @return an I_MCPResult with status, payload, and audit trail
     * @throws SecretsException if secrets cannot be resolved
     * @throws Exception on execution error
     */
    I_MCPResult invoke(IRI toolName, Model inputModel) throws SecretsException, Exception;

    /**
     * Lists all available tools exposed by this adapter.
     * 
     * @return a collection of I_MCPToolManifest
     */
    java.util.Collection<I_MCPToolManifest> listTools();

    /**
     * Retrieves the manifest for a specific tool.
     * 
     * @param toolName the name of the tool
     * @return the tool manifest, or null if not found
     */
    I_MCPToolManifest getTool(String toolName);
}
