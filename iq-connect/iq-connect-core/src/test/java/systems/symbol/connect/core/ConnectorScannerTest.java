package systems.symbol.connect.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ConnectorScannerTest {

@Test
void testScannersExecuteInDeclarationOrder() throws Exception {
List<String> calls = new ArrayList<>();
List<ConnectorScanner<List<String>>> scanners = List.of(
context -> context.add("first"),
context -> context.add("second"),
context -> context.add("third"));

for (ConnectorScanner<List<String>> scanner : scanners) {
scanner.scan(calls);
}

assertEquals(List.of("first", "second", "third"), calls);
}

@Test
void testScannerFailureStopsRemainingScanners() {
List<String> calls = new ArrayList<>();
List<ConnectorScanner<List<String>>> scanners = List.of(
context -> context.add("first"),
context -> {
throw new Exception("boom");
},
context -> context.add("third"));

Exception thrown = assertThrows(Exception.class, () -> {
for (ConnectorScanner<List<String>> scanner : scanners) {
scanner.scan(calls);
}
});

assertEquals("boom", thrown.getMessage());
assertEquals(List.of("first"), calls);
}
}