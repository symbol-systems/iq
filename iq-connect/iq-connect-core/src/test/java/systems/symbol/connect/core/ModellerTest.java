package systems.symbol.connect.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ModellerTest {

@AfterEach
void resetBaseOntology() {
Modeller.setBaseOntology("https://symbol.systems/ontology");
}

@Test
void testConnectOntologyCanBeChangedAtRuntime() {
Modeller.setBaseOntology("https://example.org/ontology");

IRI value = Modeller.connect("Connector");
assertEquals("https://example.org/ontology/connect#Connector", value.stringValue());

value = Modeller.aws("IAMUser");
assertEquals("https://example.org/ontology/aws#IAMUser", value.stringValue());

value = Modeller.github("Repository");
assertEquals("https://example.org/ontology/github#Repository", value.stringValue());
}

@Test
void testToBaseUsesDynamicConnectOntology() {
Modeller.setBaseOntology("https://demo.org/ontology/");
IRI value = Modeller.toBase("SomeType");
assertEquals("https://demo.org/ontology/connect#SomeType", value.stringValue());
}
}
