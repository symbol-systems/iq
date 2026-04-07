package systems.symbol.llm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordTokenCounter implements I_TokenCounter {

// Fallback tokenizer: splits on whitespace and punctuation boundaries.
private static final Pattern TOKEN_PATTERN = Pattern.compile("\\w+|[^\u0000-\u007F]+");

@Override
public int count(String text) {
if (text == null || text.isBlank()) {
return 0;
}
int count = 0;
Matcher matcher = TOKEN_PATTERN.matcher(text);
while (matcher.find()) {
if (!matcher.group().isBlank()) {
count++;
}
}
return count;
}
}
