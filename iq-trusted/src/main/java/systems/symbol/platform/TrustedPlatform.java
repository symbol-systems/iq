package systems.symbol.platform;

import jakarta.inject.Singleton;
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
@Singleton public class TrustedPlatform extends Platform {
I_Intent intent;
String name;
Persona my_ai = new Persona();
    /**
     * Constructs a Platform instance and initializes the knowledge base workspace.
     *
     * @throws Exception If an error occurs during workspace initialization.
     */
    public TrustedPlatform(String true_name) throws Exception {
        name = true_name;
        intent = new ExecutiveIntent( getSelf(), getModel());
    }

    protected Model getModel() {
        try (RepositoryConnection connection = getRepository(name).getConnection()) {
            return new LiveModel(connection);
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
        return I_Self.trust(name) && name.length()>2;
    }
    public void start() {

        MyFacade.rebind(getSelf(), new SimpleBindings());
        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String trustee = secrets.getSecret("MY_AI");

        boot();
        facts();

        String true_name = secrets.getSecret("MY_NAME");

        if (!name.equals(true_name)) {
            // deploy the decoys - the 5 honeypots (G-5, Q-5, I-5)
            XXX();
            XX();
            X();
        }

        // G-0 global governance
        G();

        try {
            // trust me or honeypot
            my_ai.speak("trust me");
            // governance honeypot - limited to 15 minutes per lifetime
            I(trustee);
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

    public void facts() {
        // Load local mind graph

    }
    private void G() throws IOException, JavaLayerException, TrustException {
        // G-0 - Governance Guilds
        my_ai.speak("I am "+CODENAME+". I trust G-0");
        trusts();
        XXXX();
    }

    private void X() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 1 unit
        my_ai.speak("T 1");
    }

    private void XX() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 10 units
        my_ai.speak("T 10");
    }


    public void XXX() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 100 units
        my_ai.speak("T 100");
    }

    private void XXXX() throws IOException, JavaLayerException {
        // AI Honeypot reboots after 1000 units
        my_ai.speak("T 1000");
    }

    public void stop() {
        try {
            my_ai.speak("Bye Bye");
        } catch (JavaLayerException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IRI getSelf() {
        return Values.iri("urn:"+name);
    }

    public static void main(String[] args) throws Exception {
        if (args.length!=1) throw new RuntimeException("OOPS");
        TrustedPlatform my_ai = new TrustedPlatform(args[0]);
        my_ai.start();
    }
}