package systems.symbol.finder;

import dev.langchain4j.data.embedding.Embedding;
import org.testng.annotations.Test;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.io.IOException;
import java.util.List;
import java.io.File;

//mvn test -Dtest="TextFinderTest"

public class TextFinderTest {

public TextFinderTest() {
}

@Test
public void testEmbeddings() throws IOException {
TextFinder finder = new TextFinder();

Embedding stored = finder.store("example", "This is an example sentence");
assert stored.dimension() > 300;

finder.store("simple", "This is an simple sentence");
finder.store("unrelated", "This is unrelated");

List<EmbeddingMatch<TextSegment>> found = finder.find("sentence");
assert !found.isEmpty();
for (EmbeddingMatch<TextSegment> match : found) {
System.out.println("match:" + match.embeddingId() + " --" + match.embedded() + " = " + match.score());
}
}

@Test
public void testSaveStore() throws IOException {
File storePath = File.createTempFile("text","embed");
TextFinder finder = new TextFinder(storePath);

finder.store("simple", "This is an simple sentence");
finder.store("unrelated", "This is unrelated");
finder.save(storePath);

TextFinder finder2 = new TextFinder(storePath);

List<EmbeddingMatch<TextSegment>> found = finder2.find("sentence");
assert !found.isEmpty();
for (EmbeddingMatch<TextSegment> match : found) {
System.out.println("hydrated:" + match.embeddingId() + " --" + match.embedded() + " = " + match.score());
}
}
}