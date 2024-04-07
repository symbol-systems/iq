package systems.symbol.string;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class Validate {

    public static boolean isNonAlphanumeric(String input) {
        Pattern pattern = Pattern.compile("[^\\p{Alnum}]");
        return pattern.matcher(input).find();
    }

    public static boolean isRelativePath(String string) {
        Path path = Paths.get(string);
        return !path.isAbsolute();
    }

    public static boolean isBearer(String authorizationHeader) {
        return (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer "));
    }

    public static boolean hasHost(String first) {
        try {
            String host = new URI(first).getHost();
            return host!=null && !host.isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean isSameHost(String first, String second) {
        try {
            return isSameHost(new URI(first), second);
        } catch (URISyntaxException e) {
            return false;
        }
    }
    public static boolean isSameHost(URI firstURI, String second) throws URISyntaxException {
        URI secondURI = new URI(second);
        return firstURI.getHost().equalsIgnoreCase(secondURI.getHost());
    }

    public static boolean isMissing(String topic) {
        return topic==null || topic.trim().isEmpty();
    }
}
