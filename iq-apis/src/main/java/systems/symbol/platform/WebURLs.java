package systems.symbol.platform;

import jakarta.ws.rs.core.UriInfo;

import javax.servlet.http.HttpServletRequest;

import jakarta.ws.rs.core.HttpHeaders;

public class WebURLs {

static String[] HeaderCandidates = {
"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP",
"True-Client-IP", "Forwarded", "Proxy-Client-IP", "WL-Proxy-Client-IP"
};

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

public static String getClientIP(HttpServletRequest request, HttpHeaders headers) {
String ip = request == null ? null : request.getRemoteAddr();

for (String header : HeaderCandidates) {
String ipList = headers.getHeaderString(header);
if (ipList != null && !ipList.isEmpty()) {
return ipList.split(",")[0].trim();
}
}
return ip;
}

private static String getHeader(HttpHeaders headers, String headerName, String defaultValue) {
String headerValue = headers.getHeaderString(headerName);
return headerValue != null ? headerValue : defaultValue;
}
}
