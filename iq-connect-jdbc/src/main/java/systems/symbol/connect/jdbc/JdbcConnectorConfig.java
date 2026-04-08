package systems.symbol.connect.jdbc;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for JDBC connector: connection details and discovery options.
 *
 * <p>
 * Immutable value object. Deserialized from connector config JSON:
 * </p>
 *
 * <pre>
 * {
 *   "jdbcUrl": "jdbc:postgresql://localhost:5432/mydb",
 *   "user": "app_user",
 *   "password": "${VAULT:db-password}",
 *   "autoDiscover": true,
 *   "schemas": ["public", "app"],
 *   "excludePatterns": ["test_.*", "tmp_.*"]
 * }
 * </pre>
 */
public class JdbcConnectorConfig {

  private final String jdbcUrl;
  private final String user;
  private final String password;
  private final boolean autoDiscover;
  private final List<String> schemas;
  private final List<String> excludePatterns;
  private final int connectionTimeout;
  private final int queryTimeout;

  public JdbcConnectorConfig(
      String jdbcUrl,
      String user,
      String password,
      boolean autoDiscover,
      List<String> schemas,
      List<String> excludePatterns,
      int connectionTimeout,
      int queryTimeout) {
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "jdbcUrl must not be null");
    this.user = Objects.requireNonNull(user, "user must not be null");
    this.password = Objects.requireNonNull(password, "password must not be null");
    this.autoDiscover = autoDiscover;
    this.schemas = schemas == null ? List.of() : List.copyOf(schemas);
    this.excludePatterns = excludePatterns == null ? List.of() : List.copyOf(excludePatterns);
    this.connectionTimeout = connectionTimeout > 0 ? connectionTimeout : 30;
    this.queryTimeout = queryTimeout > 0 ? queryTimeout : 300;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getUser() {
    return user;
  }

  public String getPassword() {
    return password;
  }

  public boolean isAutoDiscover() {
    return autoDiscover;
  }

  public List<String> getSchemas() {
    return schemas;
  }

  public List<String> getExcludePatterns() {
    return excludePatterns;
  }

  public int getConnectionTimeout() {
    return connectionTimeout;
  }

  public int getQueryTimeout() {
    return queryTimeout;
  }

  /**
   * Builder for fluent configuration.
   */
  public static class Builder {
    private String jdbcUrl;
    private String user;
    private String password;
    private boolean autoDiscover = true;
    private List<String> schemas = List.of();
    private List<String> excludePatterns = List.of();
    private int connectionTimeout = 30;
    private int queryTimeout = 300;

    public Builder jdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      return this;
    }

    public Builder user(String user) {
      this.user = user;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder autoDiscover(boolean autoDiscover) {
      this.autoDiscover = autoDiscover;
      return this;
    }

    public Builder schemas(List<String> schemas) {
      this.schemas = schemas;
      return this;
    }

    public Builder excludePatterns(List<String> patterns) {
      this.excludePatterns = patterns;
      return this;
    }

    public Builder connectionTimeout(int seconds) {
      this.connectionTimeout = seconds;
      return this;
    }

    public Builder queryTimeout(int seconds) {
      this.queryTimeout = seconds;
      return this;
    }

    public JdbcConnectorConfig build() {
      return new JdbcConnectorConfig(
          jdbcUrl,
          user,
          password,
          autoDiscover,
          schemas,
          excludePatterns,
          connectionTimeout,
          queryTimeout);
    }
  }
}
