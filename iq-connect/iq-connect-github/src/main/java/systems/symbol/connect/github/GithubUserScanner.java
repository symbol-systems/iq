package systems.symbol.connect.github;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import org.kohsuke.github.GHUser;

final class GithubUserScanner {

private GithubUserScanner() {
}

static void scanOrganizationUser(GHUser user, IRI orgIri, GithubScanContext context) {
try {
context.modeller().orgUser(orgIri, user.getLogin(), user.getName());
} catch (IOException e) {
// best-effort user extraction
}
}
}