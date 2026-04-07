package systems.symbol.kernel.command;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import javax.script.Bindings;
import javax.script.SimpleBindings;
import java.util.Map;

/**
 * Transport-agnostic unit of work dispatched on a live kernel realm.
 *
 * <p>Replaces the four parallel request models:
 * <ul>
 *   <li>CLI — {@code AbstractCLICommand} parameters</li>
 *   <li>REST — JAX-RS path/query params on a {@code RealmAPI} handler</li>
 *   <li>MCP — MCP tool {@code rawInput} map in {@code MCPCallContext}</li>
 *   <li>Camel — Camel {@code Exchange} body / headers</li>
 * </ul>
 *
 * <p>Surface adapters build a {@code KernelRequest} at their ingress point and
 * pass it to an {@link AbstractKernelCommand} subclass. The result comes back
 * as a {@link KernelResult}.
 */
public class KernelRequest {

private final IRI subject;   // the resource this command acts on
private final IRI caller;// authenticated principal IRI (may be null)
private final IRI realm; // realm context IRI
private final javax.script.Bindings params;   // command parameters (SPARQL bindings compatible)
private final Model   model; // optional RDF payload (may be null)
private final systems.symbol.agent.I_Command command; // optional agent command payload

private KernelRequest(Builder b) {
this.subject = b.subject;
this.caller  = b.caller;
this.realm   = b.realm;
this.params  = b.params;
this.model   = b.model;
this.command = b.command;
}

public IRI getSubject() { return subject; }
public IRI getCaller()  { return caller; }
public IRI getRealm()   { return realm; }
public Bindings getParams() { return params; }
public Model   getModel()   { return model; }
public systems.symbol.agent.I_Command getCommand() { return command; }

@Override
public String toString() {
return "KernelRequest[subject=" + subject + ", caller=" + caller + ", realm=" + realm + "]";
}

/* ── fluent builder ──────────────────────────────────────────────────── */

public static Builder on(IRI subject) {
return new Builder(subject);
}

public static final class Builder {
private final IRI subject;
private IRI  caller;
private IRI  realm;
private Bindings params  = new SimpleBindings();
private Model model   = null;
private systems.symbol.agent.I_Command command = null;

private Builder(IRI subject) { this.subject = subject; }

public Builder caller(IRI caller){ this.caller = caller; return this; }
public Builder realm(IRI realm)  { this.realm  = realm;  return this; }
public Builder model(Model model){ this.model  = model;  return this; }
public Builder command(systems.symbol.agent.I_Command cmd) { this.command = cmd; return this; }

public Builder param(String key, Object value) {
params.put(key, value);
return this;
}

public Builder params(Map<String, Object> map) {
params.putAll(map);
return this;
}

public KernelRequest build() {
return new KernelRequest(this);
}
}
}
