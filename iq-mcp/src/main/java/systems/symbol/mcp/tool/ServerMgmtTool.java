package systems.symbol.mcp.tool;

import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;
import systems.symbol.platform.runtime.RuntimeStatus;

import java.util.HashMap;
import java.util.Map;

public class ServerMgmtTool implements I_MCPTool {

    @Override
    public String getName() {
        return "server.runtime";
    }

    @Override
    public String getDescription() {
        return "Control IQ runtimes (api/mcp): start, stop, reboot, health, debug, dump";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("runtime", Map.of("type", "string", "enum", new String[]{"api", "mcp"}));
        schema.put("action", Map.of("type", "string", "enum", new String[]{"start", "stop", "reboot", "health", "debug", "dump"}));
        schema.put("debug", Map.of("type", "boolean", "default", false));
        schema.put("path", Map.of("type", "string", "default", "/tmp/iq-server-dump.tar.gz"));
        return schema;
    }

    @Override
    public I_MCPResult execute(MCPCallContext ctx, Map<String, Object> input) throws MCPException {
        String runtime = (String) input.getOrDefault("runtime", "api");
        String action = (String) input.getOrDefault("action", "health");

        if (runtime == null || action == null) {
            throw MCPException.badRequest("runtime and action are required");
        }

        switch (action) {
            case "start":
                return success(ServerRuntimeManagerFactory.getInstance().start(runtime), "started");
            case "stop":
                return success(ServerRuntimeManagerFactory.getInstance().stop(runtime), "stopped");
            case "reboot":
                return success(ServerRuntimeManagerFactory.getInstance().reboot(runtime), "rebooted");
            case "health":
                RuntimeStatus st = ServerRuntimeManagerFactory.getInstance().health(runtime);
                return MCPResult.okJson("{\"healthy\":" + st.isHealthy() + ",\"details\":\"" + st.getDetails() + "\"}");
            case "debug":
                boolean enable = (Boolean) input.getOrDefault("debug", Boolean.TRUE);
                return success(ServerRuntimeManagerFactory.getInstance().debug(runtime, enable), "debug " + (enable ? "enabled" : "disabled"));
            case "dump":
                String path = (String) input.getOrDefault("path", "/tmp/iq-server-dump.tar.gz");
                var dump = ServerRuntimeManagerFactory.getInstance().dump(runtime, path);
                return MCPResult.okJson("{\"success\":" + dump.isSuccess() + ",\"path\":\"" + dump.getPath() + "\"}");
            default:
                throw MCPException.badRequest("Unsupported action: " + action);
        }
    }

    private I_MCPResult success(boolean ok, String message) {
        if (ok) {
            return MCPResult.okJson("{\"status\":\"ok\",\"message\":\"" + message + "\"}");
        }
        return MCPResult.error(500, "failed to " + message);
    }
}
