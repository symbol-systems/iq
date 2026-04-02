package systems.symbol.runtime;

public class ServerRuntimeManagerFactory {

public static ServerRuntimeManager getInstance() {
String manager = System.getProperty("iq.runtime.manager");
if (manager == null || manager.isBlank()) {
manager = System.getenv("IQ_RUNTIME_MANAGER");
}
if (manager == null || manager.isBlank()) {
manager = "process";
}

switch (manager.toLowerCase()) {
case "quarkus":
return new QuarkusRuntimeManager();
case "default":
return new DefaultServerRuntimeManager();
case "process":
default:
return new ProcessServerRuntimeManager();
}
}

private ServerRuntimeManagerFactory() {
}
}
