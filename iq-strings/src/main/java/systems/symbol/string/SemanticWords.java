package systems.symbol.string;

import java.io.IOException;
import java.util.*;

public class SemanticWords {
    Map<String, String> vocab = new HashMap<>();

    public SemanticWords() {

    }

    public SemanticWords(Map<String, String> terms) throws IOException {
        learn(terms);
    }

    public void learn(String term, String words) throws IOException {
        String tag = Tagged.simplify(words, " ");
        vocab.put(tag, term);
    }

    public void learn(Map<String, String> terms) throws IOException {
        for( String String: terms.keySet()) {
            String words = terms.get(String);
            learn(String, words);
        }
    }

    public Set<String> parse(String text) throws IOException {
        Set<String> found = new HashSet<String>();
        Collection<String> tags = Tagged.tags(text);
//        System.out.println("tags.words:"+tags);

        String last = "";
        for( String tag: tags) {
            // single words / tags
            String means = (String) vocab.get(tag);
//            System.out.println("tags.means:"+tag+"->"+means);
            if (means!=null) found.add(means);

            // two words / ngrams
            String ngram = last+" "+tag;
            String ngram_means = (String) vocab.get(ngram);
//            System.out.println("tags.means:"+ngram+"->"+ngram_means);
            if (ngram_means!=null) found.add(ngram_means);

            last = tag;
        }
//        System.out.println("tags.found:"+tags);
        return found;
    }
}
