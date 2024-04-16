package systems.symbol.platform;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.string.Validate;

import java.io.IOException;

//@Singleton
@ApplicationScoped
public class APIPlatform extends Platform {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * Constructs a Platform instance and initializes the knowledge base workspace.
     *
     * @throws IOException & NoSuchAlgorithmException If an error occurs during workspace initialization.
     */

    public APIPlatform() throws Exception {
        super();
    }

    void onStart(@Observes StartupEvent ev) {
        log.info("platform.guarded: {} -> {}", getSelf(), !Validate.isUnGuarded());
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("platform.shutdown: {} -> {}", ev.isStandardShutdown(), ev);
        shutdown();
    }

}
