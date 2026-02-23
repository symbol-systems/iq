package systems.symbol.string;

import java.text.SimpleDateFormat;
import java.util.*;

import javax.script.Bindings;

/**
 * Utility class for manipulating and formatting strings.
 */
public class PrettyString {

private static final String LIST_SEP = ", ";

/**
 * Default constructor.
 */
public PrettyString() {
}

/**
 * Humanizes the given label by unCamelCasing and applying pretty formatting.
 *
 * @param label The input label.
 * @return Humanized and pretty-formatted string.
 */
public static String humanize(String label) {
return prettySafe(unCamelCase(label));
}

/**
 * Converts the input string to PascalCase.
 *
 * @param orgString The original string.
 * @return PascalCase formatted string.
 */
public static String pretty(String orgString) {
return toPascalCase(orgString.replaceAll("[^A-Za-z0-9]+", " "));
}

/**
 * Removes non-alphanumeric characters from the string.
 *
 * @param orgString The original string.
 * @return String with non-alphanumeric characters removed.
 */
public static String prettySafe(String orgString) {
return orgString.replaceAll("[^A-Za-z0-9 ]+", "");
}

/**
 * Sanitizes the given object's string representation by replacing
 * non-alphanumeric characters.
 *
 * @param orgString The original object's string representation.
 * @return Sanitized string.
 */
public static String sanitize(Object orgString) {
return sanitize(orgString, "-");
}

/**
 * Sanitizes the given object's string representation by replacing
 * non-alphanumeric characters with a specified string.
 *
 * @param orgString The original object's string representation.
 * @param replace   The string to replace non-alphanumeric characters.
 * @return Sanitized string.
 */
public static String sanitize(Object orgString, String replace) {
return orgString.toString().trim().replaceAll("[^A-Za-z\\.\\-0-9]+", replace).toLowerCase(Locale.ROOT);
}

public static String unCamelCase(String label) {
if (label == null)
return "";
return label.replaceAll(String.format("%s|%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
"(?<=[A-Za-z])(?=[^A-Za-z])", "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
}

public static String lamaCase(String clean_string) {
clean_string = unCamelCase(clean_string);
StringTokenizer words = new StringTokenizer(clean_string, " ");
StringBuilder camelCase = new StringBuilder();
boolean first = true;
while (words.hasMoreTokens()) {
String currentToken = sanitize(words.nextToken());
if (first)
currentToken = currentToken.toLowerCase();
else
currentToken = capitalize(currentToken);
camelCase.append(currentToken);
first = false;
}
return camelCase.toString().trim();
}

public static String toCamelCase(String word) {
String pascal = toPascalCase(word);
return pascal.substring(0, 1).toLowerCase(Locale.ROOT) + pascal.substring(1);
}

public static String toPascalCase(String clean_string) {
StringTokenizer words = new StringTokenizer(clean_string, " _-");
StringBuilder camelCase = new StringBuilder();
while (words.hasMoreTokens()) {
String currentToken = words.nextToken();
currentToken = capitalize(currentToken);
camelCase.append(currentToken);
}
return camelCase.toString().trim();
}

public static String wikize(String clean_string) {
StringTokenizer words = new StringTokenizer(clean_string, " ");
StringBuilder camelCase = new StringBuilder();
while (words.hasMoreTokens()) {
String currentToken = words.nextToken();
currentToken = capitalize(currentToken);
camelCase.append(currentToken).append(" ");
}
return sanitize(camelCase.toString().trim());
}

public static String capitalize(Object word) {
String _word = word.toString().trim();
return _word.substring(0, 1).toUpperCase() + _word.substring(1).toLowerCase();
}

public static String slice(String word, String sep) {
int ix = word.lastIndexOf(sep);
if (ix < 0)
return word;
return word.substring(0, ix);
}

public static String upto(String word, String first) {
return between(word, first, null);
}

public static String between(String word, String first, String last) {
int ix_first = word.indexOf(first);
int len_first = first.length();
if (ix_first < 0)
return word;
if (last != null) {
int ix_last = word.lastIndexOf(last);
if (ix_last < 0)
return word.substring(ix_first + len_first);
if (ix_first > ix_last)
return word;
return word.substring(ix_first + len_first, ix_last);
}
return word.substring(ix_first + len_first);
}

public static String normalizeWhitespace(String str) {
return str.replaceAll("\\s+", " ").trim();
}

public static String today(Date date, String format) {
SimpleDateFormat dt = new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss");
return dt.format(date);
}

public static String today(Date date) {
return today(date, "dd-mmm-yyyy");
}

public static String today() {
return today(new Date());
}

public static String toString(Object[] objects) {
return toString(objects, LIST_SEP);
}

public static String toString(Collection<String> objects) {
return toString(objects, LIST_SEP);
}

public static String toString(Collection<String> objects, String sep) {
if (objects == null)
return null;
StringBuilder sb = new StringBuilder();
for (String o : objects) {
if (sb.length() > 0)
sb.append(sep);
sb.append(o);

}
return sb.toString();
}

public static String toString(Object[] objects, String sep) {
if (objects == null)
return null;
StringBuilder sb = new StringBuilder();
for (int i = 0; i < objects.length; i++) {
if (objects != null) {
if (i > 0 && sep != null)
sb.append(sep);
sb.append(objects[i]);
}
}
return sb.toString();
}

public static String either(Object o, String s) {
if (o == null || o.toString().trim().isEmpty())
return s;
return o.toString();
}

public static String truncate(String s, int i) {
if (s.length() <= i)
return s;
return s.substring(0, i);
}

/**
 * Turns a non-negative number into an ordinal string used to denote the
 * position in an ordered sequence, such as 1st, 2nd,
 * 3rd, 4th.
 *
 * @param number the non-negative number
 * @return the string with the number and ordinal suffix
 */
public String ordinalize(int number) {
int remainder = number % 100;
String numberStr = Integer.toString(number);
if (11 <= number && number <= 13)
return numberStr + "th";
remainder = number % 10;
if (remainder == 1)
return numberStr + "st";
if (remainder == 2)
return numberStr + "nd";
if (remainder == 3)
return numberStr + "rd";
return numberStr + "th";
}

public static String localName(String iri) {
if (iri == null || iri.isEmpty()) {
return null;
}

String[] delimiters = { ":", "/", "v#" };
int delimiterIndex = -1;

for (String delimiter : delimiters) {
int index = iri.lastIndexOf(delimiter);
if (index > delimiterIndex) {
delimiterIndex = index;
}
}

if (delimiterIndex >= 0) {
return iri.substring(delimiterIndex + 1);
} else {
return iri;
}
}

public static String getenv(String name, String _default) {
return System.getenv(name) != null ? System.getenv(name) : _default;
}

public static int getenv(String name, int _default) {
String val = System.getenv(name);
try {
return val != null ? Integer.parseInt(val) : _default;
} catch (Exception e) {
return _default;
}
}

public static int get(Bindings bindings, String name, int _default) {
if (!bindings.containsKey(name))
return _default;
return Integer.parseInt(bindings.get(name).toString());

}

public static double get(Bindings bindings, String name, double _default) {
if (!bindings.containsKey(name))
return _default;
return Double.parseDouble(bindings.get(name).toString());

}
}
