package systems.symbol.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URLHelper {

public static URL toURL(String urlString) {
try {
return new URI(urlString).toURL();
} catch (MalformedURLException | URISyntaxException e) {
return null;
}
}

public static boolean isValidUrl(String urlString) {
try {
new URI(urlString).toURL();
return true;
} catch (URISyntaxException | MalformedURLException e) {
return false;
}
}
}
