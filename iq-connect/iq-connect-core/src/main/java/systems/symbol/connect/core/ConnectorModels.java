package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;

/**
 * Convenience helpers for working with connector state models.
 */
public final class ConnectorModels {

private ConnectorModels() {
// utility
}

/**
 * Copies all triples from {@code source} into {@code target}.
 *
 * <p>This is useful when applying a checkpoint snapshot to a target model.</p>
 */
public static void sync(Model source, Model target) {
target.clear();
for (Statement st : source) {
target.add(st);
}
}

/**
 * Returns a view of the model that contains only statements about the given subject.
 */
public static Model forSelf(Model model, org.eclipse.rdf4j.model.Resource self) {
return Models.filter(model, self, null, null);
}
}
