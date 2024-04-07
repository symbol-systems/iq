package systems.symbol.string;

import junit.framework.TestCase;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

public class TaggedTest extends TestCase {

@Test
public void testNormalize() {
String hello_world = Tagged.normalize("Hello World");
System.out.println("tags.normalize:"+hello_world);
assert hello_world.equals("hello-world");
}

@Test
public void testToStems() throws IOException {
String s = "Running there Stopped and Waited";
Collection<String> tags = Tagged.stems(s);
System.out.println("tags.stems:"+tags);
assert tags.contains("Stop");
assert tags.contains("Wait");
assert tags.contains("Run");
}

@Test
public void testToStemmedTags() throws IOException {
String s = "Running there Stopped and Waited";
Collection<String> tags = Tagged.tags(s);
System.out.println("tags.stemmed-tags:"+tags);
assert tags.contains("stop");
assert tags.contains("wait");
assert tags.contains("run");
}
}