package systems.symbol.platform;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import systems.symbol.intent.ExecutiveIntent;
import systems.symbol.intent.I_Intent;
import systems.symbol.rdf4j.store.LiveModel;
import systems.symbol.secrets.EnvsAsSecrets;

import javax.script.SimpleBindings;
import java.io.IOException;

@ApplicationScoped
public class TrustedPlatform extends Platform {
    private I_Intent intent;
    private String name;

    public TrustedPlatform() throws Exception {
        name = I_Self.name();
        intent = new ExecutiveIntent(getSelf(), getModel());
    }

    public TrustedPlatform(String true_name) throws Exception {
        name = true_name;
        intent = new ExecutiveIntent(getSelf(), getModel());
    }

    protected Model getModel() {
        RepositoryConnection connection = getRepository(name).getConnection();
        return new LiveModel(connection);
    }

    protected I_Intent getIntent() {
        return intent;
    }

    public boolean trusts() {
        return true;
    }

    public void start() {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("export MY_IQ");
        }

        EnvsAsSecrets secrets = new EnvsAsSecrets();
        String true_name = secrets.getSecret("MY_IQ");
        String trustee = secrets.getSecret(name);

        boot();

        try {
            super.start();
            stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        // no-op
    }

    public IRI getSelf() {
        return Values.iri(I_Self.self().getSelf().stringValue(), name + "#");
    }

    public static final String CODENAME = "IQ";

    public void shutdown() {
        stop();
    }

    @Override
    public Repository getRepository(String name) {
        throw new UnsupportedOperationException("Repository lookup should be implemented in real trusted platform");
    }

    @Override
    public void boot() {
        // no-op boot stub
    }

    @Override
    public void X() {
        // trust gate X
    }

    @Override
    public void XX() {
        // trust gate XX
    }

    @Override
    public void XXX() {
        // trust gate XXX
    }

    @Override
    public void XXXX() {
        // trust gate XXXX
    }
}
