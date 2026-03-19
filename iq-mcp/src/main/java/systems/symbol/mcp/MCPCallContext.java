package systems.symbol.mcp;

import systems.symbol.kernel.pipeline.KernelCallContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * MCPCallContext — per-request envelope threaded through the
 * {@link systems.symbol.mcp.connect.MCPConnectPipeline}.
 *
 * <p>Extends {@link KernelCallContext} to inherit all cross-cutting fields:
 * <ul>
 *   <li>{@link #traceId()} / {@link #startTime()} — immutable, set at construction</li>
 *   <li>{@link #principal()} / {@link #realm()} / {@link #jwt()} — set by {@code AuthGuardMiddleware}</li>
 *   <li>{@link #isAuthorised()} — set to {@code true} by {@code ACLFilterMiddleware}</li>
 * </ul>
 *
 * <p>MCP-specific additions:
 * <ul>
 *   <li>{@link #toolName()} — the MCP tool identifier for this request</li>
 *   <li>{@link #rawInput()} — immutable input parameters from the MCP client</li>
 * </ul>
 *
 * <p>All attributes are stored via the inherited {@link #set}/{@link #get}/{@link #has}
 * bag; typed accessors are provided for the most common fields. Never share a
 * context across concurrent requests.
 */
public final class MCPCallContext extends KernelCallContext {

/* ── MCP-specific attribute keys ──────────────────────────────────────── */
public static final String KEY_TOOL   = "mcp.tool";
public static final String KEY_QUOTA_USED = "mcp.quotaUsed";

/* ── Kernel-aligned aliases (backward compat for middleware code) ──────── *
 * These point to the canonical KernelCallContext key strings so that   *
 * existing middleware using MCPCallContext.KEY_PRINCIPAL etc. reads/writes  *
 * the same attribute slot as the kernel's own typed accessors. */
public static final String KEY_PRINCIPAL  = KernelCallContext.KEY_PRINCIPAL;
public static final String KEY_REALM  = KernelCallContext.KEY_REALM;
public static final String KEY_AUTHORISED = KernelCallContext.KEY_AUTHORISED;
public static final String KEY_JWT= KernelCallContext.KEY_JWT;
public static final String KEY_ROLES  = KernelCallContext.KEY_ROLES;

/** Input parameters as received from the MCP client (immutable snapshot). */
private final Map<String, Object> rawInput;

/**
 * Creates a new context for a single MCP tool invocation.
 *
 * @param toolName the MCP tool identifier (e.g. {@code "sparql.query"})
 * @param rawInput the raw parameter map from the MCP protocol layer
 */
public MCPCallContext(String toolName, Map<String, Object> rawInput) {
super(); // sets traceId (final), startTime (final), KEY_AUTHORISED=false
this.rawInput = Collections.unmodifiableMap(new HashMap<>(rawInput));
set(KEY_TOOL, toolName);
}

/* ── MCP-specific accessors ────────────────────────────────────────────── */

/** The MCP tool name for this request. */
public String toolName() { return get(KEY_TOOL); }

/** Raw input map from the protocol layer (unmodifiable). */
public Map<String, Object> rawInput() { return rawInput; }

// Inherited from KernelCallContext:
//   traceId()  – unique trace ID (UUID, final)
//   startTime()– Instant the context was created (final)
//   principal()– authenticated principal string
//   realm()– realm IRI
//   jwt()  – raw bearer token
//   isAuthorised() – authorisation flag
//   set/get/has– attribute bag
//   attributes()   – read-only view of all attributes

@Override
public String toString() {
return "MCPCallContext{tool=" + toolName() + ", trace=" + traceId() + ", realm=" + realm() + "}";
}
}
