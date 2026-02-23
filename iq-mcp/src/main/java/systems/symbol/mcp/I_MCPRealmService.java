package systems.symbol.mcp;

import java.util.Collection;
import java.util.Optional;
import org.eclipse.rdf4j.model.IRI;
import systems.symbol.realm.I_Realm;

/**
 * MCP Server interface for managing MCP adapters and tool registry.
 * 
 * The MCPServer acts as a coordinator:
 * - Registers/unregisters adapters (e.g., FactAdapter, AgentAdapter, LlmAdapter)
 * - Routes MCP tool calls to appropriate adapter
 * - Maintains global tool registry (all tools from all adapters)
 * - Enforces cross-realm authorization policies
 * 
 * Typically deployed as a Quarkus REST endpoint that implements the MCP protocol
 * and delegates tool execution to registered adapters.
 */
public interface I_MCPRealmService extends systems.symbol.platform.I_Self {

    public IRI getSelf();
    public I_Realm getRealm();

    /**
     * Registers an MCP adapter with this server.
     * 
     * The adapter's tools (via listTools()) are added to the global registry.
     * 
     * @param adapter the I_MCPAdapter to register
     * @return true if adapter registered successfully
     */
    boolean registerAdapter(I_MCPAdapter adapter);

    /**
     * Unregisters an MCP adapter from this server.
     * 
     * The adapter's tools are removed from the global registry.
     * 
     * @param adapter the I_MCPAdapter to unregister
     * @return true if adapter was registered and now unregistered
     */
    boolean unregisterAdapter(I_MCPAdapter adapter);

    /**
     * Retrieves all registered adapters.
     * 
     * @return collection of I_MCPAdapter instances
     */
    Collection<I_MCPAdapter> getAdapters();

    /**
     * Retrieves the adapter responsible for a named tool.
     * 
     * @param toolName the tool name (e.g., "sparql.query")
     * @return Optional adapter that provides this tool
     */
    Optional<I_MCPAdapter> getAdapterForTool(String toolName);

    /**
     * Retrieves all tools available across all registered adapters.
     * 
     * @return collection of I_MCPToolManifest from all adapters
     */
    Collection<I_MCPToolManifest> listAllTools();

    /**
     * Retrieves a tool manifest by name.
     * 
     * @param toolName the tool name
     * @return Optional tool manifest
     */
    Optional<I_MCPToolManifest> getTool(String toolName);

    /**
     * Invokes a named tool via the appropriate adapter.
     * 
     * @param toolName the tool name
     * @param input the input Model (RDF or JSON)
     * @return I_MCPResult with outcome
     * @throws Exception if tool not found or execution fails
     */
    I_MCPResult invokeTool(String toolName, org.eclipse.rdf4j.model.Model input) throws Exception;
}
