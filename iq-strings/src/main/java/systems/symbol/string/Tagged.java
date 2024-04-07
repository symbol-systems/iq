package systems.symbol.string;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class Tagged {
static Locale locale = Locale.ROOT;

public static String normalize(String text) {
return text.trim().toLowerCase(locale).replaceAll(" ", "-");
}

public static String simplify(String text, String sep) throws IOException {
text = text.toLowerCase(locale);
StringBuilder sb = new StringBuilder();
Collection<String> tags = stems(text);
for(String tag: tags) {
sb.append(tag).append(sep);
};
return sb.toString().trim();
}

public static Collection<String> tags(String text) throws IOException {
Collection<String> stems = stems(text);
Collection<String> tagged = new ArrayList<>();
stems.forEach( stem -> {
tagged.add( normalize(stem));
});
return tagged;
}

public static Collection<String> stems(String text) throws IOException {
// tokenize text, remove stop word, stemming
Collection<String> tokens = new ArrayList<String>();
AttributeFactory factory = AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;
Tokenizer tokenizer = new StandardTokenizer(factory);
tokenizer.setReader(new StringReader(text));
CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
TokenStream stopFilter = new StopFilter(tokenizer, stopWords);
//StringBuilder sb = new StringBuilder();
CharTermAttribute charTermAttribute = tokenizer.addAttribute(CharTermAttribute.class);
PorterStemFilter tokenStream = new PorterStemFilter(stopFilter);
tokenStream.reset();
while (tokenStream.incrementToken()) {
String term = charTermAttribute.toString();

tokens.add(term);
//sb.append(term + " ");
}
tokenStream.end();
tokenStream.close();

tokenizer.close();
//	System.out.println("QU="+ sb.toString());
return tokens;
}
}
