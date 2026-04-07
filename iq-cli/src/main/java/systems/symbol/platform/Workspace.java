package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import systems.symbol.platform.I_Self;

import java.io.File;
import java.io.IOException;

public class Workspace implements I_Self {
private final IRI self;
private final File home;
private final Repository repository;

public Workspace(File home) throws IOException {
this.home = home;
if (!home.exists() && !home.mkdirs()) {
throw new IOException("Unable to create workspace folder: " + home);
}

this.self = resolveIRI(home);

File repositories = new File(home, "repositories");
if (!repositories.exists() && !repositories.mkdirs()) {
throw new IOException("Unable to create repositories folder: " + repositories);
}

File repositoryDir = new File(repositories, "default");
this.repository = new SailRepository(new NativeStore(repositoryDir));
this.repository.init();

File assets = new File(home, "assets"); if (!assets.exists()) assets.mkdirs();
File backups = new File(home, "backups"); if (!backups.exists()) backups.mkdirs();
File pub = new File(home, "public"); if (!pub.exists()) pub.mkdirs();
}

@Override
public IRI getSelf() {
return self;
}

public Repository getCurrentRepository() {
return repository;
}

public String getCurrentRepositoryName() {
return "default";
}

public String getStoreType() {
return "native";
}

private static IRI resolveIRI(File home) {
String env = System.getenv("IQ");
if (env != null && !env.isBlank()) {
return SimpleValueFactory.getInstance().createIRI(env.endsWith(":" ) ? env : env + ":");
}
try {
return SimpleValueFactory.getInstance().createIRI(home.toURI().toString());
} catch (Exception e) {
return SimpleValueFactory.getInstance().createIRI("iq:");
}
}
}
