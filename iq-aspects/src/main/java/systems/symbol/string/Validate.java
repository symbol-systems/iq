package systems.symbol.string;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Validate {

public static boolean isUnGuarded() {
return !isMissing(System.getenv("MY_IQ_UNGUARDED"));
}

public static boolean isNonAlphanumeric(String string) {
if (isMissing(string))
return true;
Pattern pattern = Pattern.compile("[^\\p{Alnum}]");
return pattern.matcher(string).find();
}

public static boolean isRelativePath(String thing) {
if (isMissing(thing))
return false;
Path path = Paths.get(thing);
return !path.isAbsolute();
}

public static boolean isBearer(String header) {
return (header != null && header.toLowerCase().startsWith("bearer "));
}

public static boolean hasHost(String url) {
if (isMissing(url))
return false;
try {
String host = new URI(url).getHost();
return host != null && !host.isEmpty();
} catch (URISyntaxException e) {
return false;
}
}

public static boolean isSameHost(String thing1, String thing2) {
try {
return isSameHost(new URI(thing1), new URI(thing2));
} catch (URISyntaxException e) {
return false;
}
}

public static boolean isSameHost(URI thing1, URI thing2) throws URISyntaxException {
if (isMissing(thing1) || isMissing(thing2))
return false;
return thing1.getHost().equalsIgnoreCase(thing2.getHost());
}

public static boolean isMissing(Object thing) {
return thing == null || thing.toString().trim().isEmpty();
}

public static boolean isURN(String thing) {
return thing != null && thing.contains(":");
}

/**
 * Check the URL matches the Swagger-style pattern
 * (`/v1/example{param1}/{param2}`)
 */
public static boolean isSwaggerPath(String pattern, String url) {
String regexPattern = pattern.replaceAll("\\{[^/]+\\}", "[^/]+");
return url.matches(regexPattern);
}

public static boolean contains(String[] array, String value) {
for (String s : array) {
if (s.equals(value)) {
return true;
}
}
return false;
}

}
