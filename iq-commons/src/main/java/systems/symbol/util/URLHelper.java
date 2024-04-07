package systems.symbol.util;

import java.net.MalformedURLException;
import java.net.URL;

public class URLHelper {

    public static boolean isValidUrl(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
