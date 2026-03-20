package systems.symbol.connect.core;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Utility for working with RDF models in connectors.
 *
 * <p>Provides helper methods for common modeling patterns, such as creating connector state models
 * or generating IRIs for connector resources.</p>
 */
public final class Modeller {

private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

private static volatile String baseOntology = "https://symbol.systems/ontology";
private static volatile String connectOntology = baseOntology + "/connect#";
private static volatile String awsOntology = baseOntology + "/aws#";
private static volatile String githubOntology = baseOntology + "/github#";
public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

public static String getBaseOntology() {
return baseOntology;
}

public static void setBaseOntology(String newBaseOntology) {
if (newBaseOntology == null || newBaseOntology.trim().isEmpty()) {
throw new IllegalArgumentException("baseOntology must be non-empty");
}
String normalized = newBaseOntology.endsWith("/") ? newBaseOntology.substring(0, newBaseOntology.length() - 1) : newBaseOntology;
baseOntology = normalized;
connectOntology = baseOntology + "/connect#";
awsOntology = baseOntology + "/aws#";
githubOntology = baseOntology + "/github#";
}

public static String getConnectOntology() {
return connectOntology;
}

public static String getAwsOntology() {
return awsOntology;
}

public static String getGithubOntology() {
return githubOntology;
}

private final Model model;

private Modeller(Model model) {
this.model = model;
}

public static Modeller create(Model model) {
return new Modeller(model);
}

public static IRI rdfType() {
return VF.createIRI(RDF_TYPE);
}

public static IRI connect(String localName) {
return VF.createIRI(getConnectOntology() + localName);
}

public static IRI aws(String localName) {
return VF.createIRI(getAwsOntology() + localName);
}

public static IRI github(String localName) {
return VF.createIRI(getGithubOntology() + localName);
}

public static IRI iri(String absoluteUri) {
return VF.createIRI(absoluteUri);
}

public static IRI toDomainURN(String domain, String localName) {
return VF.createIRI("urn:" + domain + ":" + localName);
}

public static IRI toSPIFFE(String domain, String localName) {
return VF.createIRI("spiffe://" + domain + "/" + localName);
}

public static IRI toBase(String localName) {
return VF.createIRI(getConnectOntology() + localName);
}
}
