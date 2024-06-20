package systems.symbol.platform;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import systems.symbol.agent.ExecutiveAgent;
import systems.symbol.agent.I_Agent;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.intent.JSR233;
import systems.symbol.intent.Select;
import systems.symbol.intent.Update;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.I_Secrets;
import systems.symbol.string.Validate;

import javax.script.Bindings;
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
log.info("apis.onStart: {} -> {}", getSelf(), !Validate.isUnGuarded());

}

void onStop(@Observes ShutdownEvent ev) {
log.info("apis.onStop: {} -> {}", ev.isStandardShutdown(), ev);
shutdown();
}

}