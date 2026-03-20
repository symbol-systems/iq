package systems.symbol.connect.github;

import org.eclipse.rdf4j.model.IRI;

final class GithubScanContext {

private final IRI connectorId;
private final GithubModeller modeller;

GithubScanContext(IRI connectorId, GithubModeller modeller) {
this.connectorId = connectorId;
this.modeller = modeller;
}

IRI connectorId() {
return connectorId;
}

GithubModeller modeller() {
return modeller;
}
}