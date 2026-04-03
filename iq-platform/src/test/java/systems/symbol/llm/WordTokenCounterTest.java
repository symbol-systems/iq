package systems.symbol.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WordTokenCounterTest {

@Test
void countWithWhitespaceAndPunctuation() {
WordTokenCounter counter = new WordTokenCounter();
assertEquals(5, counter.count("Hello, world! This is test."));
assertEquals(0, counter.count(""));
assertEquals(1, counter.count("foo"));
assertEquals(3, counter.count("one two three"));
}

}
