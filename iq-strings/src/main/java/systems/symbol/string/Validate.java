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
    public static boolean isNonAlphanumeric(String input) {
        if (isMissing(input)) return true;
        Pattern pattern = Pattern.compile("[^\\p{Alnum}]");
        return pattern.matcher(input).find();
    }

    public static boolean isRelativePath(String pathy) {
        if (isMissing(pathy)) return false;
        Path path = Paths.get(pathy);
        return !path.isAbsolute();
    }

    public static boolean isBearer(String header) {
        return (header != null && header.toLowerCase().startsWith("bearer "));
    }

    public static boolean hasHost(String url) {
        if (isMissing(url)) return false;
        try {
            String host = new URI(url).getHost();
            return host!=null && !host.isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static boolean isSameHost(String first, String second) {
        try {
            return isSameHost(new URI(first), new URI(second));
        } catch (URISyntaxException e) {
            return false;
        }
    }
    public static boolean isSameHost(URI firstURI, URI secondURI) throws URISyntaxException {
        if (isMissing(firstURI) || isMissing(secondURI)) return false;
        return firstURI.getHost().equalsIgnoreCase(secondURI.getHost());
    }

    public static boolean isMissing(Object topic) {
        return topic==null || topic.toString().trim().isEmpty();
    }
}
