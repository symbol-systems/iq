package systems.symbol.runtime;

public final class RuntimeStatus {
private final String runtimeType;
private final boolean healthy;
private final String details;

public RuntimeStatus(String runtimeType, boolean healthy, String details) {
this.runtimeType = runtimeType;
this.healthy = healthy;
this.details = details;
}

public String getRuntimeType() {
return runtimeType;
}

public boolean isHealthy() {
return healthy;
}

public String getDetails() {
return details;
}

@Override
public String toString() {
return "RuntimeStatus{" +
"runtimeType='" + runtimeType + '\'' +
", healthy=" + healthy +
", details='" + details + '\'' +
'}';
}
}
