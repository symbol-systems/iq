package systems.symbol.mcp;

import java.util.Collection;
import java.util.Optional;

import org.eclipse.rdf4j.model.Model;

/**
 * Backwards-compatible alias: tests previously referenced `MCPRealmService` by
 * name. This class wraps `MCPService` to preserve that API.
 */
public class MCPRealmService implements I_MCPService {
public MCPRealmService() {
}

@Override
public boolean registerAdapter(I_MCPAdapter adapter) {
// TODO Auto-generated method stub
throw new UnsupportedOperationException("Unimplemented method 'registerAdapter'");
}

@Override
public boolean unregisterAdapter(I_MCPAdapter adapter) {
// TODO Auto-generated method stub
throw new UnsupportedOperationException("Unimplemented method 'unregisterAdapter'");
}

@Override
public Collection<I_MCPAdapter> getAdapters() {
// TODO Auto-generated method stub
throw new UnsupportedOperationException("Unimplemented method 'getAdapters'");
}

@Override
public Optional<I_MCPAdapter> getAdapterForTool(String toolName) {
// TODO Auto-generated method stub
throw new UnsupportedOperationException("Unimplemented method 'getAdapterForTool'");
}

@Override
public Collection<I_MCPToolManifest> listAllTools() {
// TODO Auto-generated method stub
throw new UnsupportedOperationException("Unimplemented method 'listAllTools'");
}

@Override
public I_MCPResult invokeTool(String toolName, Model input) throws Exception {
// TODO Auto-generated method stub
throw new UnsupportedOperationException("Unimplemented method 'invokeTool'");
}
}
