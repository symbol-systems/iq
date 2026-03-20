package systems.symbol.kernel;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.kernel.agent.I_AgentRegistry;
import systems.symbol.kernel.agent.SimpleAgentRegistry;
import systems.symbol.kernel.event.I_EventHub;
import systems.symbol.platform.I_Self;
import systems.symbol.secrets.I_Secrets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Fluent builder that constructs and drives the {@link I_Kernel} lifecycle.
 *
 * <p>Replaces the boot logic currently scattered across:
 * <ul>
 *   <li>{@code RealmPlatform.onStart()} — {@code iq-apis}</li>
 *   <li>{@code CLIContext.init()} — {@code iq-cli}</li>
 *   <li>Quarkus CDI producers in {@code iq-mcp}</li>
 * </ul>
 *
 * <p>Usage (all surfaces):
 * <pre>{@code
 * KernelContext ctx = KernelBuilder.create()
 *     .withSelf(I_Self.self())
 *     .withSecrets(System::getenv)  // or new EnvsAsSecrets() from iq-platform
 *     .withHome(new File(System.getenv().getOrDefault("IQ_HOME", ".iq")))
 *     .build()
 *     .start()
 *     .getContext();
 * }</pre>
 *
 * <p>Surface layers (Quarkus, Picocli, MCP SDK) should call {@code build()} during
 * their own startup hooks and cache the returned {@link I_Kernel} instance for
 * the process lifetime. They are responsible for calling {@code stop()} on shutdown.
 */
public class KernelBuilder {

    private static final Logger log = LoggerFactory.getLogger(KernelBuilder.class);

    private IRI       self;
    private I_Secrets secrets    = System::getenv;  // reads process env vars by default
    private File      home       = defaultHome();
    private I_AgentRegistry agentRegistry = null;
    private final Map<String, Object> attributes = new HashMap<>();

    private KernelBuilder() {}

    /** Entry point. */
    public static KernelBuilder create() {
        return new KernelBuilder();
    }

    /**
     * Sets the canonical self IRI. Defaults to {@link I_Self#self()}.
     */
    public KernelBuilder withSelf(I_Self self) {
        this.self = self.getSelf();
        return this;
    }

    public KernelBuilder withSelf(IRI self) {
        this.self = self;
        return this;
    }

    /**
     * Sets the secrets provider. Defaults to {@code System::getenv} (environment variables).
     */
    public KernelBuilder withSecrets(I_Secrets secrets) {
        this.secrets = secrets;
        return this;
    }

    /**
     * Sets the home directory ({@code .iq/} root).
     * Defaults to {@code $IQ_HOME} env var or {@code .iq} relative to cwd.
     */
    public KernelBuilder withHome(File home) {
        this.home = home;
        return this;
    }

    /**
     * Stores an arbitrary attribute in the {@link KernelContext} attribute bag.
     * Used by surface layers to attach singletons (e.g. a Realm reference) after build.
     */
    public KernelBuilder withAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    public I_AgentRegistry getAgentRegistry() {
        return this.agentRegistry != null
                ? this.agentRegistry
                : new SimpleAgentRegistry();
    }

    /**
     * Installs an event hub into the kernel context.
     */
    public KernelBuilder withEventHub(I_EventHub eventHub) {
        this.attributes.put("eventHub", eventHub);
        return this;
    }

    /**
     * Installs an agent registry into the kernel context.
     */
    public KernelBuilder withAgentRegistry(I_AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
        return this;
    }

    /**
     * Builds and returns a ready-to-start {@link I_Kernel}.
     * Does not call {@link I_Kernel#start()} — the caller is responsible.
     *
     * @throws KernelBootException if required configuration is missing
     */
    public I_Kernel build() {
        IRI resolvedSelf = self != null ? self : I_Self.self().getSelf();
        String version = resolveVersion();

        log.info("kernel.build: {} @ {} v{}", resolvedSelf, home.getAbsolutePath(), version);

        I_AgentRegistry registry = this.agentRegistry != null
                ? this.agentRegistry
                : new systems.symbol.kernel.agent.SimpleAgentRegistry();

        KernelContext ctx = new KernelContext(resolvedSelf, secrets, home, version,
                Map.copyOf(attributes), registry);
        return new DefaultKernel(ctx);
    }

    /* ── private helpers ──────────────────────────────────────────────────── */

    private static File defaultHome() {
        String env = System.getenv("IQ_HOME");
        return new File(env != null ? env : ".iq");
    }

    private static String resolveVersion() {
        try (InputStream in = KernelBuilder.class
                .getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if (in == null) return "dev";
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("Implementation-Version");
            return v != null ? v : "dev";
        } catch (IOException e) {
            return "dev";
        }
    }

    /* ── default kernel implementation ────────────────────────────────────── */

    private static final class DefaultKernel implements I_Kernel {
        private static final Logger log = LoggerFactory.getLogger(DefaultKernel.class);
        private final KernelContext ctx;
        private volatile boolean started = false;

        DefaultKernel(KernelContext ctx) { this.ctx = ctx; }

        @Override
        public IRI getSelf() { return ctx.getSelf(); }

        @Override
        public void start() throws Exception {
            if (started) return;
            log.info("kernel.start: {}", ctx.getSelf());
            ctx.getHome().mkdirs();
            started = true;
            log.info("kernel.ready: {} @ {}", ctx.getSelf(), ctx.getHome().getAbsolutePath());
        }

        @Override
        public void stop() {
            if (!started) return;
            log.info("kernel.stop: {}", ctx.getSelf());
            started = false;
        }

        @Override
        public KernelContext getContext() {
            if (!started) throw new KernelBootException(
                    "kernel.context.not-started",
                    "KernelContext accessed before start() was called");
            return ctx;
        }
    }
}
