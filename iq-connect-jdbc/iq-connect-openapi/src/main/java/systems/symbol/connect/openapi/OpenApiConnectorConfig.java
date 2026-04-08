package systems.symbol.connect.openapi;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for OpenAPI connector: spec URL, auth, and discovery options.
 *
 * <p>
 * Immutable value object. Deserialized from connector config JSON:
 * </p>
 *
 * <pre>
 * {
 *   "specUrl": "https://api.stripe.com/openapi/stripe-api.json",
 *   "endpoints": ["customers", "charges", "subscriptions"],
 *   "authType": "bearer",
 *   "authToken": "${VAULT:stripe-api-key}",
 *   "basePath": "https://api.stripe.com/v1",
 *   "rateLimitPerSecond": 10,
 *   "cacheResults": true
 * }
 * </pre>
 */
public class OpenApiConnectorConfig {

  private final String specUrl;
  private final String basePath;
  private final List<String> endpoints;
  private final String authType;
  private final String authToken;
  private final int rateLimitPerSecond;
  private final boolean cacheResults;
  private final int readTimeout;

  public OpenApiConnectorConfig(
      String specUrl,
      String basePath,
      List<String> endpoints,
      String authType,
      String authToken,
      int rateLimitPerSecond,
      boolean cacheResults,
      int readTimeout) {
    this.specUrl = Objects.requireNonNull(specUrl, "specUrl must not be null");
    this.basePath = basePath != null ? basePath : "";
    this.endpoints = endpoints == null ? List.of() : List.copyOf(endpoints);
    this.authType = authType != null ? authType : "none";
    this.authToken = authToken;
    this.rateLimitPerSecond = rateLimitPerSecond > 0 ? rateLimitPerSecond : 10;
    this.cacheResults = cacheResults;
    this.readTimeout = readTimeout > 0 ? readTimeout : 30;
  }

  public String getSpecUrl() {
    return specUrl;
  }

  public String getBasePath() {
    return basePath;
  }

  public List<String> getEndpoints() {
    return endpoints;
  }

  public String getAuthType() {
    return authType;
  }

  public String getAuthToken() {
    return authToken;
  }

  public int getRateLimitPerSecond() {
    return rateLimitPerSecond;
  }

  public boolean isCacheResults() {
    return cacheResults;
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  /** Builder for fluent configuration. */
  public static class Builder {
    private String specUrl;
    private String basePath = "";
    private List<String> endpoints = List.of();
    private String authType = "none";
    private String authToken;
    private int rateLimitPerSecond = 10;
    private boolean cacheResults = true;
    private int readTimeout = 30;

    public Builder specUrl(String specUrl) {
      this.specUrl = specUrl;
      return this;
    }

    public Builder basePath(String basePath) {
      this.basePath = basePath;
      return this;
    }

    public Builder endpoints(List<String> endpoints) {
      this.endpoints = endpoints;
      return this;
    }

    public Builder authType(String authType) {
      this.authType = authType;
      return this;
    }

    public Builder authToken(String authToken) {
      this.authToken = authToken;
      return this;
    }

    public Builder rateLimitPerSecond(int limit) {
      this.rateLimitPerSecond = limit;
      return this;
    }

    public Builder cacheResults(boolean cache) {
      this.cacheResults = cache;
      return this;
    }

    public Builder readTimeout(int seconds) {
      this.readTimeout = seconds;
      return this;
    }

    public OpenApiConnectorConfig build() {
      return new OpenApiConnectorConfig(
          specUrl,
          basePath,
          endpoints,
          authType,
          authToken,
          rateLimitPerSecond,
          cacheResults,
          readTimeout);
    }
  }
}
