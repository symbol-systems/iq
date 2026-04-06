
package systems.symbol.connect.parquet;

import java.io.File;
import java.time.Instant;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorModels;
import systems.symbol.connect.core.Modeller;

public final class ParquetConnector extends AbstractConnector {

private final ParquetConnectorConfig config;

public ParquetConnector(String connectorId, ParquetConnectorConfig config) {
super(connectorId,
  new LinkedHashModel(),
  Values.iri(connectorId + "/graph/current"),
  Values.iri(Modeller.getConnectOntology()),
  Values.iri("urn:parquet:"));
this.config = config;
}

@Override
public ConnectorMode getMode() { return ConnectorMode.READ_ONLY; }

@Override
protected void doRefresh() throws Exception {
if (config.getApiKey().isEmpty()) {
throw new IllegalStateException("PARQUET_FILE_PATH is required");
}

String filePath = config.getApiKey().get();
validateParquetFile(filePath);

IRI entity = Values.iri(entityBaseIri().stringValue() + "parquet-item");
getModel().add(entity, Modeller.rdfType(), Values.iri(ontologyBaseIri().stringValue() + "ParquetResource"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "service"), Values.***REMOVED***("Parquet"), graphIri());
getModel().add(entity, Values.iri(ontologyBaseIri().stringValue() + "lastSeen"), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.HAS_RESOURCE), entity, graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.LAST_SYNCED_AT), Values.***REMOVED***(Instant.now().toString()), graphIri());
getModel().add(getConnectorId(), Values.iri(ConnectorModels.RESOURCE_COUNT), Values.***REMOVED***(1), graphIri());
}

private void validateParquetFile(String filePath) throws Exception {
File file = new File(filePath);
if (!file.exists()) {
throw new IllegalStateException("Parquet file not found: " + filePath);
}
if (!file.isFile()) {
throw new IllegalStateException("Path is not a file: " + filePath);
}
if (!file.canRead()) {
throw new IllegalStateException("Parquet file is not readable: " + filePath);
}
String name = filePath.toLowerCase();
if (!name.endsWith(".parquet") && !name.endsWith(".parq")) {
throw new IllegalStateException("File does not appear to be a Parquet file: " + filePath);
}
}
}
