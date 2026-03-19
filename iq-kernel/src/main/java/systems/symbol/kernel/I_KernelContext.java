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
 */
public interface I_KernelContext extends I_Self {

    public IRI getSelf();

    public I_Secrets getSecrets();

    public File getHome();

    public String getVersion();

    public <T> T get(String key);

    public boolean has(String key);

}
