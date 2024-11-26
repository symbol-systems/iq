package systems.symbol.tools;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

public class Online {
    static String server = "https://www.google.com";

    public static boolean isOnline() {
        return isOnline(server);
    }

    /**
     * Checks if the system is online by sending a HEAD request to a reliable
     * server.
     *
     * @return true if the system is online, false otherwise.
     */
    public static boolean isOnline(String server) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(server))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
