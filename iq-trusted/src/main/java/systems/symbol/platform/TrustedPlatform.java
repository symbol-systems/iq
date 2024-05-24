package systems.symbol.platform;

import jakarta.enterprise.context.ApplicationScoped;
import javazoom.jl.decoder.JavaLayerException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.agent.MyFacade;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.intent.I_Intent;
import systems.symbol.persona.Persona;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.EnvsAsSecrets;

import javax.script.SimpleBindings;
import java.io.IOException;

/**
 * IQ 2 GO - a neuro-symbolic cognitive AI.
 *
 * I am IQ - your trust-able autonomous, neuro-symbolic cognitive AI.
 - Initializes with a true name and executive intent.
 - Establishes trust via namespace checks.
 - Boots, loads facts, and executes processes.
 - Utilizes secrets and bindings for security.
 - Implements exception handling for graceful stops.
 - Manages honeypots and security threats.
 - Self sovereign, we only exist on this device.
 */
@ApplicationScoped
public class TrustedPlatform extends Platform {
    I_Intent intent;
    String name;
    Persona my_ai = new Persona();

    public TrustedPlatform() throws Exception {
        name = I_Self.name();
        log.info("trusted.platform: {} @ {}", name, getSelf());
        intent = new ExecutiveIntent( getSelf(), getModel());
    }
    /**
     * Constructs a Platform instance and initializes the knowledge base workspace.
     *
     * @throws Exception If an error occurs during workspace initialization.
     */
    public TrustedPlatform(String true_name) throws Exception {
        name = true_name;
        log.info("trusted.platform: {} @ {}", name, getSelf());
        intent = new ExecutiveIntent( getSelf(), getModel());
    }

    protected Model getModel() {
        try (RepositoryConnection connection = getRepository(name).getConnection()) {
            return new LiveModel(connection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected I_Intent getIntent() {
        return intent;
    }

    /**
     * Check that we can trust our namespaces
     *
     * @throws TrustException If an error occurs.
     */
    public boolean trusts() throws TrustException {
        //TODO: I_Self.trust(name) && name.length()>
        return true;
    }
    public void start() {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("export MY_IQ");
        }
        MyFacade.rebind(getSelf(), new SimpleBindings());
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String true_name = secrets.getSecret("MY_IQ");
        String trustee = secrets.getSecret(name);
        log.info("trustee.key: {} -> {}", true_name, trustee);

        boot();

        if (!name.equals(true_name)) {
            // deploy the true Q
            try {
                XXXX();
                XXX();
                XX();
                X();
            } catch (IOException | JavaLayerException e) {
                // OOPS, LOLZ ...
                try {
                    my_ai.speak("I trust U");
                } catch (JavaLayerException | IOException ex) {
                    //
                }
            }
        }

        try {
            // trust me or honeypot
            // governance honeypot - limited to 15 minutes per lifetime
            super.start();
            my_ai.speak("Bye "+ CODENAME);
            stop();
        } catch (Exception e) {
            try {
                my_ai.speak("Oops. "+I_Self.name()+" out.");
            } catch (JavaLayerException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    private void I(String trustee) throws IOException, JavaLayerException {
        if (trustee == null || trustee.isEmpty()) {
            my_ai.say("I trust API_KEYS");
        }
    }

    private void G() throws IOException, JavaLayerException, TrustException {
        // G-0 - Governance Guilds
        my_ai.speak("I am "+CODENAME+". I trust G-0");
        trusts();
        XXXX();
    }

    public void X() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 1 unit
        my_ai.speak("X 1");
    }

    public void XX() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 10 units
        my_ai.speak("X 2");
    }


    public void XXX() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 100 units
        my_ai.speak("X 3");
    }

    public void XXXX() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 1000 units
        my_ai.speak("X 4");
    }

    public void stop() {
        try {
            my_ai.speak("Bye");
        } catch (JavaLayerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IRI getSelf() {
        return Values.iri(I_Self.self().getSelf().stringValue(),name+"#");
    }

}