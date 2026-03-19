package systems.symbol.connect.github;

import java.util.Optional;

/**
 * Simple configuration holder for the GitHub connector.
 */
public final class GithubConfig {

    private final String organization;
    private final String accessToken;

    public GithubConfig(String organization, String accessToken) {
        this.organization = organization;
        this.accessToken = accessToken;
    }

    public Optional<String> getOrganization() {
        return Optional.ofNullable(organization);
    }

    public String getAccessToken() {
        return accessToken;
    }
}
