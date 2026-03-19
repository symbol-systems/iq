package systems.symbol.kernel.event;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event emitted or consumed via an {@link I_EventHub}.
 *
 * <p>The payload is intentionally untyped at this level — surface layers
 * use the {@code contentType} hint to deserialise. The three canonical
 * payload types are {@link Model} (RDF graph), {@code Bindings} (SPARQL
 * binding set), and {@code String} (text / JSON).
 */
public final class KernelEvent {

private final String  id;
private final IRI topic;
private final IRI source;
private final Instant timestamp;
private final String  contentType;
private final Object  payload;

private KernelEvent(Builder b) {
this.id  = b.id;
this.topic   = b.topic;
this.source  = b.source;
this.timestamp   = b.timestamp;
this.contentType = b.contentType;
this.payload = b.payload;
}

public String  getId()  { return id; }
public IRI getTopic()   { return topic; }
public IRI getSource()  { return source; }
public Instant getTimestamp()   { return timestamp; }
public String  getContentType() { return contentType; }

@SuppressWarnings("unchecked")
public <T> T getPayload()   { return (T) payload; }

/** Convenient typed accessor — throws {@link ClassCastException} if type mismatches. */
public <T> T getPayload(Class<T> type) { return type.cast(payload); }

public Model asModel() { return getPayload(Model.class); }

@Override
public String toString() {
return "KernelEvent[" + id + ", topic=" + topic + ", src=" + source + "]";
}

/* ── builder ─────────────────────────────────────────────────────────── */

public static Builder on(IRI topic) { return new Builder(topic); }

public static final class Builder {
private final IRI topic;
private String  id  = UUID.randomUUID().toString();
private IRI source  = null;
private Instant timestamp   = Instant.now();
private String  contentType = "application/octet-stream";
private Object  payload = null;

private Builder(IRI topic) { this.topic = topic; }

public Builder id(String id)   { this.id  = id;  return this; }
public Builder source(IRI source)  { this.source  = source;  return this; }
public Builder timestamp(Instant ts)   { this.timestamp   = ts;  return this; }
public Builder contentType(String ct)  { this.contentType = ct;  return this; }
public Builder payload(Object payload) { this.payload = payload; return this; }
public Builder rdf(Model model){
this.payload = model;
this.contentType = "application/ld+json";
return this;
}
public Builder text(String text)   {
this.payload = text;
this.contentType = "text/plain";
return this;
}

public KernelEvent build() { return new KernelEvent(this); }
}
}
