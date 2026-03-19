package systems.symbol.kernel;

import org.eclipse.rdf4j.model.IRI;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of the live kernel state after {@link KernelBuilder} completes.
 *
 * <p>A {@code KernelContext} is created once per process (or once per test) and
 * threaded through the command chain. Command handlers and middleware receive it
 * read-only; they must not mutate it.
 *
 * <p>Surface-specific state (HTTP request, MCP tool name, Camel exchange, …) is
 * carried in per-request types ({@link systems.symbol.kernel.pipeline.KernelCallContext},
 * {@link systems.symbol.kernel.command.KernelRequest}) — not here.
 */
public final class KernelContext implements I_KernelContext {

private final IRI   self;
private final I_Secrets secrets;
private final File  home;
private final Stringversion;
private final Map<String, Object> attributes;

KernelContext(IRI self, I_Secrets secrets, File home, String version,
  Map<String, Object> attributes) {
this.self   = self;
this.secrets= secrets;
this.home   = home;
this.version= version;
this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
}

/** The canonical IRI of this running instance (from {@code MY_IQ} env or default). */
@Override
public IRI getSelf() { return self; }

/** Secrets provider wired at boot time (env vars, VFS vault, …). */
public I_Secrets getSecrets() { return secrets; }

/** Root {@code .iq/} home directory. {@code null} in stateless / cloud deployments. */
public File getHome() { return home; }

/** Implementation version from MANIFEST.MF, or {@code "dev"} if not in a JAR. */
public String getVersion() { return version; }

/**
 * Extension attributes set during build (e.g. surface-specific singletons).
 * Use typed accessors on subclass contexts rather than raw attribute reads
 * in production code.
 */
@SuppressWarnings("unchecked")
public <T> T get(String key) { return (T) attributes.get(key); }

public boolean has(String key) { return attributes.containsKey(key); }

@Override
public String toString() {
return "KernelContext[" + self + " @ " + home + " v" + version + "]";
}
}
