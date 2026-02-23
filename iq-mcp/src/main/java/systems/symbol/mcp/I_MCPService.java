package systems.symbol.mcp;

import java.util.Collection;
import java.util.Optional;

/**
 * Minimal MCP coordinator contract used by tests and server implementations.
 * Keeps the API small and stable (6 methods) so tests can validate interface
 * presence.
 */
public interface I_MCPService {

    boolean registerAdapter(I_MCPAdapter adapter);

    boolean unregisterAdapter(I_MCPAdapter adapter);

    Collection<I_MCPAdapter> getAdapters();

    Optional<I_MCPAdapter> getAdapterForTool(String toolName);

    Collection<I_MCPToolManifest> listAllTools();

    I_MCPResult invokeTool(String toolName, org.eclipse.rdf4j.model.Model input) throws Exception;
}
