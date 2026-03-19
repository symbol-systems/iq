package systems.symbol.kernel.pipeline;

import org.eclipse.rdf4j.model.IRI;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-request envelope threaded through a {@link I_Pipeline}.
 *
 * <p>Carries the cross-cutting fields meaningful for every surface: trace
 * identity, authentication outcome, realm IRI, and an extensible attribute bag.
 * Surface-specific layers extend this class to add their own typed fields.
 *
 * <h4>Extension pattern (iq-mcp)</h4>
 * <pre>{@code
 * public final class MCPCallContext extends KernelCallContext {
 *     private final String toolName;
 *     private final Map<String,Object> rawInput; // immutable
 * }
 * }</pre>
 *
 * <p>Rules: {@code traceId} and {@code startTime} are immutable after
 * construction. All other fields are set by middleware via {@link #set}.
 * Never share a context across concurrent requests.
 */
public class KernelCallContext {

    /* ── well-known attribute keys ───────────────────────────────────────── */
    public static final String KEY_PRINCIPAL  = "kernel.principal";  // String
    public static final String KEY_REALM      = "kernel.realm";      // IRI
    public static final String KEY_AUTHORISED = "kernel.authorised"; // Boolean
    public static final String KEY_ROLES      = "kernel.roles";      // List<String>
    public static final String KEY_JWT        = "kernel.jwt";        // String (raw bearer)

    private final String  traceId;
    private final Instant startTime;
    private final Map<String, Object> attributes = new HashMap<>();

    public KernelCallContext() {
        this.traceId   = UUID.randomUUID().toString();
        this.startTime = Instant.now();
        set(KEY_AUTHORISED, false);
    }

    /** Unique trace identifier for this request (UUID, set at construction). */
    public String  traceId()    { return traceId; }
    /** Timestamp when this context was created. */
    public Instant startTime()  { return startTime; }
    /** Authenticated principal identifier, or {@code null} if not yet set. */
    public String  principal()  { return get(KEY_PRINCIPAL); }
    /** Realm IRI set by an auth middleware, or {@code null}. */
    public IRI     realm()      { return get(KEY_REALM); }
    /** Raw JWT bearer string, or {@code null}. */
    public String  jwt()        { return get(KEY_JWT); }
    /** Whether an auth middleware has positively authorised this request. */
    public boolean isAuthorised() {
        Boolean b = get(KEY_AUTHORISED);
        return b != null && b;
    }

    /* ── generic attribute store ─────────────────────────────────────────── */

    public void set(String key, Object value) { attributes.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) attributes.get(key); }

    public boolean has(String key) { return attributes.containsKey(key); }

    /** Read-only view for audit / serialisation. */
    public Map<String, Object> attributes() { return Collections.unmodifiableMap(attributes); }

    @Override
    public String toString() {
        return getClass().getSimpleName()
               + "[trace=" + traceId + ", realm=" + realm() + ", auth=" + isAuthorised() + "]";
    }
}
