package systems.symbol.platform.runtime;

public class ServerRuntimeManagerFactory {

    public static ServerRuntimeManager getInstance() {
        String manager = System.getProperty("iq.runtime.manager");
        if (manager == null || manager.isBlank()) {
            manager = System.getenv("IQ_RUNTIME_MANAGER");
        }
        if (manager == null || manager.isBlank()) {
            manager = "memory";
        }

        switch (manager.toLowerCase()) {
            case "quarkus":
                return new QuarkusRuntimeManager();
            case "default":
                return new DefaultServerRuntimeManager();
            case "memory":
            case "process":  // Legacy alias
            default:
                return new InMemoryServerRuntimeManager();
        }
    }
}
