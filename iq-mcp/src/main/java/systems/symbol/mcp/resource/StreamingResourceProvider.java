package systems.symbol.mcp.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Streaming support for MCP resources.
 *
 * <p>Enables HTTP 206 Partial Content responses for large resources.
 * Clients can request byte ranges using the Range header:
 * <pre>
 * GET /graphql/schema
 * Range: bytes=0-9999
 * </pre>
 *
 * <p>Server responds with:
 * <pre>
 * HTTP/1.1 206 Partial Content
 * Content-Length: 10000
 * Content-Range: bytes 0-9999/50000
 * Accept-Ranges: bytes
 * </pre>
 *
 * <p>Usage:
 * <pre>
 * String content = getResourceContent();
 * String rangeHeader = request.getHeader("Range");
 * String partial = StreamingResourceProvider.applyRangeSupport(content, rangeHeader);
 * Map headers = StreamingResourceProvider.getRangeHeaders(content, rangeHeader);
 * </pre>
 */
public class StreamingResourceProvider {

private static final Logger log = LoggerFactory.getLogger(StreamingResourceProvider.class);

/**
 * Parse HTTP Range header (e.g., "bytes=0-999").
 *
 * @param rangeHeader the Range header value
 * @param totalSize the total size of the resource
 * @return Range object with start and end, or null if invalid
 */
public static Range parseRangeHeader(String rangeHeader, long totalSize) {
if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
return null;
}

try {
String range = rangeHeader.substring(6);  // Remove "bytes="
String[] parts = range.split("-");

if (parts.length != 2) {
return null;
}

long start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
long end = parts[1].isEmpty() ? totalSize - 1 : Long.parseLong(parts[1]);

// Validate range
if (start < 0 || end >= totalSize || start > end) {
return null;
}

return new Range(start, end, totalSize);
} catch (NumberFormatException ex) {
log.warn("[StreamingResourceProvider] invalid range header: {}", rangeHeader);
return null;
}
}

/**
 * Build HTTP 206 response headers for partial content.
 *
 * @param range the requested range
 * @return Map of HTTP headers
 */
public static Map<String, String> buildPartialContentHeaders(Range range) {
Map<String, String> headers = new HashMap<>();
headers.put("Content-Length", String.valueOf(range.length()));
headers.put("Content-Range", String.format("bytes %d-%d/%d", range.start, range.end, range.total));
headers.put("Accept-Ranges", "bytes");
return headers;
}

/**
 * Extract substring for a given range from content.
 *
 * @param content the full resource content
 * @param range the byte range
 * @return substring for the range
 */
public static String extractRange(String content, Range range) {
byte[] bytes = content.getBytes();
int end = (int) Math.min(range.end + 1, bytes.length);
int start = (int) Math.min(range.start, bytes.length);
return new String(java.util.Arrays.copyOfRange(bytes, start, end));
}

/**
 * Apply byte-range filtering to content.
 *
 * @param content the original resource content
 * @param rangeHeader the Range header from the request
 * @return partial content if range was specified, otherwise full content
 */
public static String applyRangeSupport(String content, String rangeHeader) {
if (rangeHeader == null || content == null) {
return content;
}

Range range = parseRangeHeader(rangeHeader, content.length());
if (range == null) {
return content;
}

String partialContent = extractRange(content, range);
log.debug("[StreamingResourceProvider] returning {} bytes ({}-{})", 
range.length(), range.start, range.end);

return partialContent;
}

/**
 * Get HTTP headers for a byte-range response.
 *
 * @param content the full resource content
 * @param rangeHeader the Range header from the request
 * @return Map of headers, or empty map if no range
 */
public static Map<String, String> getRangeHeaders(String content, String rangeHeader) {
if (rangeHeader == null || content == null) {
return new HashMap<>();
}

Range range = parseRangeHeader(rangeHeader, content.length());
if (range == null) {
return new HashMap<>();
}

return buildPartialContentHeaders(range);
}

/**
 * Byte range specification.
 */
public static class Range {
public final long start;
public final long end;
public final long total;

public Range(long start, long end, long total) {
this.start = start;
this.end = end;
this.total = total;
}

/**
 * Number of bytes in this range.
 */
public long length() {
return end - start + 1;
}

@Override
public String toString() {
return String.format("Range(bytes %d-%d of %d)", start, end, total);
}
}
}
