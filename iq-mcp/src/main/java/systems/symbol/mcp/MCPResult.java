package systems.symbol.mcp;

/**
 * MCPResult — Concrete, immutable implementation of {@link I_MCPResult}.
 *
 * <p>Use the static factory methods rather than constructing directly:
 * <pre>
 * I_MCPResult r = MCPResult.ok("{\"bindings\":[...]}", "application/json");
 * I_MCPResult e = MCPResult.error(403, "Permission denied");
 * </pre>
 */
public final class MCPResult implements I_MCPResult {

private final boolean error;
private final String  content;
private final String  mimeType;
private final int errorCode;

private MCPResult(boolean error, int errorCode, String content, String mimeType) {
this.error = error;
this.errorCode = errorCode;
this.content   = content;
this.mimeType  = mimeType;
}

/* ── factories ───────────────────────────────────────────────────────── */

public static MCPResult ok(String content, String mimeType) {
return new MCPResult(false, 0, content, mimeType);
}

public static MCPResult okJson(String json) {
return ok(json, "application/json");
}

public static MCPResult okTurtle(String turtle) {
return ok(turtle, "text/turtle");
}

public static MCPResult okText(String text) {
return ok(text, "text/plain");
}

public static MCPResult error(int code, String message) {
String body = "{\"error\":{\"code\":" + code + ",\"message\":" + jsonStr(message) + "}}";
return new MCPResult(true, code, body, "application/json");
}

public static MCPResult notFound(String uri) {
return error(404, "Resource not found: " + uri);
}

/* ── I_MCPResult ─────────────────────────────────────────────────────── */

@Override public boolean isError(){ return error; }
@Override public String  getContent() { return content; }
@Override public String  getMimeType(){ return mimeType; }
@Override public int getErrorCode(){ return errorCode; }

@Override
public String toString() {
return "MCPResult{error=" + error + ", mimeType='" + mimeType + "', content='" + content + "'}";
}

/* ── helpers ─────────────────────────────────────────────────────────── */

private static String jsonStr(String s) {
return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
}
}
