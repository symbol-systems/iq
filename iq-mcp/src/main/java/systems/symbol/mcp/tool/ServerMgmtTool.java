package systems.symbol.mcp.tool;

import systems.symbol.mcp.I_MCPResult;
import systems.symbol.mcp.I_MCPTool;
import systems.symbol.mcp.MCPCallContext;
import systems.symbol.mcp.MCPException;
import systems.symbol.mcp.MCPResult;
import systems.symbol.platform.runtime.ServerRuntimeManagerFactory;
import systems.symbol.platform.runtime.RuntimeStatus;

import java.util.HashMap;
import java.util.List;
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
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "runtime", Map.of("type", "string", "enum", List.of("api", "mcp"), "description", "Runtime to control: 'api' or 'mcp'"),
            "action", Map.of("type", "string", "enum", List.of("start", "stop", "reboot", "health", "debug", "dump"), "description", "Action to perform"),
            "debug", Map.of("type", "boolean", "default", false, "description", "Enable/disable debug for debug action"),
            "path", Map.of("type", "string", "default", "/tmp/iq-server-dump.tar.gz", "description", "Path for dump file")
        ));
        schema.put("required", List.of("runtime", "action"));
        return schema;
    }

    @Override
    public Map<String, Object> getOutputSchema() {
        return Map.of(
            "type", "object",
            "description", "Runtime control result",
            "properties", Map.of(
                "status", Map.of("type", "string", "enum", List.of("ok", "error"), "description", "Operation status"),
                "message", Map.of("type", "string", "description", "Response message"),
                "healthy", Map.of("type", "boolean", "description", "Health status (for health action)"),
                "details", Map.of("type", "string", "description", "Additional details"),
                "path", Map.of("type", "string", "description", "Dump file path (for dump action)")
            )
        );
    }

    @Override
    public List<Map<String, Object>> getExamples() {
        return List.of(
            Map.of(
                "description", "Check API runtime health",
                "input", Map.of("runtime", "api", "action", "health"),
                "output", Map.of("healthy", true, "details", "All systems operational")
            ),
            Map.of(
                "description", "Start MCP runtime",
                "input", Map.of("runtime", "mcp", "action", "start"),
                "output", Map.of("status", "ok", "message", "started")
            ),
            Map.of(
                "description", "Dump server state for debugging",
                "input", Map.of("runtime", "api", "action", "dump", "path", "/tmp/iq-dump.tar.gz"),
                "output", Map.of("status", "ok", "path", "/tmp/iq-dump.tar.gz")
            )
        );
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
