package systems.symbol.runtime;

public final class ServerDump {
private final String runtimeType;
private final String path;
private final boolean success;
private final String message;

public ServerDump(String runtimeType, String path, boolean success, String message) {
this.runtimeType = runtimeType;
this.path = path;
this.success = success;
this.message = message;
}

public String getRuntimeType() {
return runtimeType;
}

public String getPath() {
return path;
}

public boolean isSuccess() {
return success;
}

public String getMessage() {
return message;
}

@Override
public String toString() {
return "ServerDump{" +
"runtimeType='" + runtimeType + '\'' +
", path='" + path + '\'' +
", success=" + success +
", message='" + message + '\'' +
'}';
}
}
