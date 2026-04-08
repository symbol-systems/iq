package systems.symbol.rdf4j.fedx;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.sail.Sail;

public interface FedXRepositoryMaterializationHook {
    void applyMaterialization(Repository repository);
}
