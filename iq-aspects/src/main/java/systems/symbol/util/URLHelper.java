package systems.symbol.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class URLHelper {

    public static boolean isValidUrl(String urlString) {
        try {
            new URI(urlString).toURL();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }
}
