package systems.symbol.platform.runtime;

public class ServerRuntimeManagerFactory {

private static final ServerRuntimeManager INSTANCE = new DefaultServerRuntimeManager();

public static ServerRuntimeManager getInstance() {
return INSTANCE;
}

private ServerRuntimeManagerFactory() {
}
}
