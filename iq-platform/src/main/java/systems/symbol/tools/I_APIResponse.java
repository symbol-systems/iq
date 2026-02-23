package systems.symbol.tools;

import jakarta.ws.rs.core.Response;

public interface I_APIResponse<T> {
public T asData();

public Response.Status getStatus();
}
