package systems.symbol.string;

import org.junit.jupiter.api.Test;

public class PrettyStringTest {

@Test
public void testHumanize() {
}

@Test
public void testPretty() {
}

@Test
public void testPrettySafe() {
}

@Test
public void testSanitize() {
assert PrettyString.sanitize("hello").equals("hello");
assert PrettyString.sanitize("hello.hbs").equals("hello.hbs");
assert PrettyString.sanitize("hello-hbs").equals("hello-hbs");
assert PrettyString.sanitize("hello hbs").equals("hello-hbs");
assert PrettyString.sanitize("Hello HBS").equals("hello-hbs");
assert PrettyString.sanitize("  HelLo   HbS  ").equals("hello-hbs");
}

@Test
public void testSanitizeUC() {
}

@Test
public void testUnCamelCase() {
}

@Test
public void testLamaCase() {
}

@Test
public void testToCamelCase() {
String hello_world = PrettyString.toCamelCase("hello world");
System.out.println("testToCamelCase: " + hello_world);
assert hello_world.equals("helloWorld");
}

@Test
public void testToPascalCase() {
String hello_world = PrettyString.toPascalCase("hello world");
System.out.println("testToPascalCase: " + hello_world);
assert hello_world.equals("HelloWorld");
}

@Test
public void testWikize() {
}

@Test
public void testCapitalize() {
String hello_world = PrettyString.capitalize("hello world");
System.out.println("testCapitalise: " + hello_world);
assert hello_world.equals("Hello world");
}
}