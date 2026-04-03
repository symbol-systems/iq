package systems.symbol.connect.template;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import systems.symbol.connect.core.ConnectorGraphModeller;

/**
 * Minimal template modeller for sample object mapping into connector graph.
 */
public final class TemplateModeller extends ConnectorGraphModeller {

public TemplateModeller(Model model, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
super(model, graphIri, ontologyBaseIri, entityBaseIri);
}

public IRI createTemplateItem(String itemId, String name, String description) {
IRI item = entity("item", itemId);
addType(item, "TemplateItem");
addLiteral(item, "name", name);
addLiteral(item, "description", description);
return item;
}
}
