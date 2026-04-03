package systems.symbol.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServerRuntimeManager implements ServerRuntimeManager {

private static final Logger log = LoggerFactory.getLogger(DefaultServerRuntimeManager.class);

@Override
public boolean start(String runtimeType) {
log.info("[default] start {}", runtimeType);
return true;
}

@Override
public boolean stop(String runtimeType) {
log.info("[default] stop {}", runtimeType);
return true;
}

@Override
public boolean reboot(String runtimeType) {
log.info("[default] reboot {}", runtimeType);
return true;
}

@Override
public RuntimeStatus health(String runtimeType) {
log.info("[default] health {}", runtimeType);
return new RuntimeStatus(runtimeType, true, "ok");
}

@Override
public boolean debug(String runtimeType, boolean enable) {
log.info("[default] debug {} -> {}", runtimeType, enable);
return true;
}

@Override
public ServerDump dump(String runtimeType, String path) {
log.info("[default] dump {} -> {}", runtimeType, path);
return new ServerDump(runtimeType, path, true, "dumped to " + path);
}
}
