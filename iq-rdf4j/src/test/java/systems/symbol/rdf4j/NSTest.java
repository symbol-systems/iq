package systems.symbol.rdf4j;

import systems.symbol.COMMONS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.testng.annotations.Test;

public class NSTest {

@Test
public void testDefaults() {
NS ns = NS.defaults();
assert ns != null;
assert ns.baseNS != null;
}

@Test
public void testGetBaseNS() {
NS ns = NS.defaults();
assert ns.getBaseNS() == ns.baseNS;
}

@Test
public void testBaseNS() {
assert NS.defaults().baseNS.equalsIgnoreCase(COMMONS.IQ_NS_TEST);
}

@Test
public void testDefaultPrefixes() {
   assert NS.defaults().prefix2ns.size()>0; // SKOS, DCT + default
}

@Test
public void testGetNS() {
assert NS.defaults().getNS(SKOS.PREFIX).equals(SKOS.NAMESPACE);
}

@Test
public void testGetPrefix() {
String prefix = NS.defaults().findPrefix(SKOS.PREF_LABEL.stringValue());
assert prefix.equals(SKOS.PREFIX);
}

@Test
public void testLocalize() {
assert NS.defaults().localize(SKOS.PREF_LABEL.stringValue()).equals("skos:prefLabel");
}

@Test
public void testGlobalize() {
String globalize = NS.defaults().globalize("skos:prefLabel");
System.out.println("rdf4j.ns.globalize: "+globalize+" = "+SKOS.PREF_LABEL);
assert globalize.equals(SKOS.PREF_LABEL.stringValue());

globalize = NS.defaults().globalize(NS.IQ_NS_TEST);
System.out.println("rdf4j.ns.globalize.root: "+globalize);
assert globalize.equals(NS.IQ_NS_TEST);
}

@Test
public void testAdd() {
}

@Test
public void testFindPrefix() {
assert NS.defaults().findPrefix(SKOS.PREF_LABEL.stringValue() ) == "skos";
}

@Test
public void testContains() {
assert NS.defaults().contains(SKOS.PREF_LABEL.stringValue());
}

}