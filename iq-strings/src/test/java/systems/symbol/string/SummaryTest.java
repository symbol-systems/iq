package systems.symbol.string;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SummaryTest {


@Test
public void testSummary() throws IOException {
FileReader fileReader = new FileReader(new File("src/test/resources/assets/sample.txt"));
String text = ToString.toString(fileReader);
String summary = Summary.summary(text, 1);
//System.out.println("Summary.1: "+summary);
assert summary.contains("data management");
}
}