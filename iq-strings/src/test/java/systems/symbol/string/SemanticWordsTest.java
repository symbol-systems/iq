package systems.symbol.string;

import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

public class SemanticWordsTest {

    @Test
    public void testParse() throws IOException {
        SemanticWords trie = new SemanticWords();
        trie.learn( "urn:hello", "Hello");
        trie.learn( "urn:world", "World");
        trie.learn( "urn:greetings", "greetings");
        trie.learn( "urn:two-words", "Two Words");
        System.out.println("parse.vocab: "+trie.vocab);
        assert trie.vocab.size() == 4;

        Set<String> semantics = trie.parse("Hello World");
        System.out.println("parse.knows: "+semantics);
        assert semantics.contains("urn:hello");
        assert semantics.contains("urn:world");

        semantics = trie.parse("Bob greets Alice");
        System.out.println("parse.knows: "+semantics);
        assert semantics.contains("urn:greetings");

        semantics = trie.parse("Bob says two words");
        System.out.println("parse.knows: "+semantics);
        assert semantics.contains("urn:two-words");
    }

    @Test
    void testParseFile() throws IOException {
        SemanticWords semantics = new SemanticWords();
        semantics.learn( "data-governance", "Data Governance");
        semantics.learn( "data-management", "Data Management");

        FileReader fileReader = new FileReader(new File("src/test/resources/assets/sample.txt"));
        String text = ToString.toString(fileReader);
        Set<String> found = semantics.parse(text);
        System.out.println("parse.knows: "+found);
        assert found.contains("data-governance");
        assert found.contains("data-management");
    }
}