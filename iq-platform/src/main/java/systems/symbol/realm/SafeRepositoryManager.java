package systems.symbol.realm;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SafeRepositoryManager extends LocalRepositoryManager {
 Map<String, Repository> repositories = new HashMap<>();

public SafeRepositoryManager(File home) {
super(home);
}

@Override
public Repository getRepository(String name) {
Repository repository = repositories.get(name);
if (repository != null) return repository;
repository = super.getRepository(name);
repositories.put(name, repository);
return repository;
}
}
