package systems.symbol.platform;

import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.HttpHeaders;

public class WebURLs {

public static String getFullURL(UriInfo uriInfo, HttpHeaders headers) {
String baseUrl = getRequestURL(uriInfo, headers);
String requestUri = uriInfo.getPath();
String query = uriInfo.getRequestUri().getQuery();

return query != null ? baseUrl + requestUri + "?" + query : baseUrl + requestUri;
}

public static String getRequestURL(UriInfo uriInfo, HttpHeaders headers) {
String forwardedProto = getHeader(headers, "X-Forwarded-Proto", uriInfo.getRequestUri().getScheme());
String forwardedHost = getHeader(headers, "X-Forwarded-Host", uriInfo.getRequestUri().getHost());
String forwardedPort = getHeader(headers, "X-Forwarded-Port",
String.valueOf(uriInfo.getRequestUri().getPort()));

String port = forwardedPort.equals("-1") || forwardedPort.isEmpty() ? "" : ":" + forwardedPort;
return forwardedProto + "://" + forwardedHost + port + "/";
}

public static String getClientHostname(HttpHeaders headers) {
String forwardedFor = getHeader(headers, "X-Forwarded-For", null);
if (forwardedFor != null) {
String[] parts = forwardedFor.split(",");
return parts[0].trim(); // First IP in the list, typically the client's real IP
}
return getHeader(headers, "X-Real-IP", null); // Fallback to X-Real-IP if available
}

private static String getHeader(HttpHeaders headers, String headerName, String defaultValue) {
String headerValue = headers.getHeaderString(headerName);
return headerValue != null ? headerValue : defaultValue;
}
}
