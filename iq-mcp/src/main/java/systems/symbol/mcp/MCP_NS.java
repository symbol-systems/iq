package systems.symbol.mcp;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

/**
 * MCP_NS — Namespace constants for the MCP integration layer.
 *
 * <p>All MCP-specific IRIs live under {@code urn:mcp:} and
 * IQ extension IRIs remain under {@code https://symbol.systems/iq/mcp/}.
 *
 * <p>Named-graph conventions:
 * <ul>
 *   <li>{@code mcp:audit}   — audit trail for every tool call</li>
 *   <li>{@code mcp:policy}  — access-control rules</li>
 *   <li>{@code mcp:quota}   — per-principal rate/budget counters</li>
 *   <li>{@code mcp:pipeline} — middleware configuration</li>
 *   <li>{@code mcp:cache}   — materialised read-only tool results</li>
 * </ul>
 */
public interface MCP_NS {

/* ── base namespaces ── */
String MCP   = "urn:mcp:";
String IQ_MCP = "https://symbol.systems/iq/mcp/";

/* ── well-known named graphs ── */
IRI GRAPH_AUDIT   = Values.iri(MCP, "audit");
IRI GRAPH_POLICY  = Values.iri(MCP, "policy");
IRI GRAPH_QUOTA   = Values.iri(MCP, "quota");
IRI GRAPH_CONNECT = Values.iri(MCP, "connect");
IRI GRAPH_CACHE   = Values.iri(MCP, "cache");

/* ── tool / resource taxonomy ── */
IRI Tool= Values.iri(MCP, "Tool");
IRI Resource= Values.iri(MCP, "Resource");
IRI Prompt  = Values.iri(MCP, "Prompt");
IRI Middleware  = Values.iri(MCP, "Middleware");

/* ── tool properties ── */
IRI toolName= Values.iri(MCP, "toolName");
IRI toolDescription = Values.iri(MCP, "toolDescription");
IRI toolSchema  = Values.iri(MCP, "toolSchema");
IRI toolRateLimit   = Values.iri(MCP, "toolRateLimit");
IRI isAudited   = Values.iri(MCP, "isAudited");
IRI isReadOnly  = Values.iri(MCP, "isReadOnly");

/* ── middleware properties ── */
IRI order   = Values.iri(MCP, "order");
IRI enabled = Values.iri(MCP, "enabled");
IRI middlewareClass = Values.iri(MCP, "middlewareClass");

/* ── audit properties ── */
IRI auditPrincipal  = Values.iri(MCP, "principal");
IRI auditTool   = Values.iri(MCP, "tool");
IRI auditTimestamp  = Values.iri(MCP, "timestamp");
IRI auditDuration   = Values.iri(MCP, "durationMs");
IRI auditOutcome= Values.iri(MCP, "outcome");
IRI auditRealm  = Values.iri(MCP, "realm");

/* ── policy / ACL properties ── */
IRI allowedRole = Values.iri(MCP, "allowedRole");
IRI deniedTool  = Values.iri(MCP, "deniedTool");
IRI allowedTool = Values.iri(MCP, "allowedTool");

/* ── quota properties ── */
IRI quotaLimit  = Values.iri(MCP, "quotaLimit");
IRI quotaWindow = Values.iri(MCP, "quotaWindow");
IRI quotaUsed   = Values.iri(MCP, "quotaUsed");
IRI quotaResetAt= Values.iri(MCP, "quotaResetAt");

/* ── JWT / auth config (for mcp:connect middleware) ── */
IRI jwtSecret   = Values.iri(MCP, "jwtSecret");
IRI jwtIssuer   = Values.iri(MCP, "jwtIssuer");
IRI jwtAudience = Values.iri(MCP, "jwtAudience");
IRI jwksUri = Values.iri(MCP, "jwksUri");

/* ── IQ-specific resource URI templates ── */
String IQ_RESOURCE_REALM_SCHEMA= "iq://realm/{realm}/schema";
String IQ_RESOURCE_REALM_POLICY= "iq://realm/{realm}/policy";
String IQ_RESOURCE_REALM_SHACL = "iq://realm/{realm}/shacl";
String IQ_RESOURCE_SELF_NAMESPACES = "iq://self/namespaces";
String IQ_RESOURCE_SELF_REALMS = "iq://self/realms";
String IQ_RESOURCE_SELF_VOID   = "iq://self/void";

/* ── tool name constants ── */
String TOOL_SPARQL_QUERY= "sparql.query";
String TOOL_SPARQL_UPDATE   = "sparql.update";
String TOOL_RDF_DESCRIBE= "rdf.describe";
String TOOL_RDF_WALK= "rdf.walk";
String TOOL_ACTOR_TRIGGER   = "actor.trigger";
String TOOL_REALM_SCHEMA= "realm.schema";
String TOOL_REALM_STATUS= "realm.status";
String TOOL_REALM_SEARCH= "realm.search";
String TOOL_LLM_INVOKE  = "llm.invoke";
String TOOL_LLM_STATUS  = "llm.status";
}
