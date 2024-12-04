package systems.symbol.platform;

import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.HttpHeaders;

public class WebURLs {

    public static String getFullURL(UriInfo uriInfo, HttpHeaders headers) {
        String baseUrl = getBaseURL(uriInfo, headers);

        String requestUri = uriInfo.getPath();
        String query = uriInfo.getRequestUri().getQuery();
        return query != null ? baseUrl + requestUri + "?" + query : baseUrl + requestUri;
    }

    public static String getBaseURL(UriInfo uriInfo, HttpHeaders headers) {
        String forwardedProto = headers.getHeaderString("X-Forwarded-Proto");
        String forwardedHost = headers.getHeaderString("X-Forwarded-Host");
        String forwardedPort = headers.getHeaderString("X-Forwarded-Port");

        String scheme = forwardedProto != null ? forwardedProto : uriInfo.getBaseUri().getScheme();
        String host = forwardedHost != null ? forwardedHost : uriInfo.getBaseUri().getHost();
        String port = forwardedPort != null
                ? ":" + forwardedPort
                : (uriInfo.getBaseUri().getPort() == -1 ? "" : ":" + uriInfo.getBaseUri().getPort());

        return scheme + "://" + host + port + "/";
    }
}
