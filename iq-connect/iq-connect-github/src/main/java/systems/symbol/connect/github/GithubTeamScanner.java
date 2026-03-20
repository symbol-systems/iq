package systems.symbol.connect.github;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GHUser;

final class GithubTeamScanner {

    private GithubTeamScanner() {
    }

    static void scan(GHTeam team, IRI orgIri, String orgLogin, GithubScanContext context) {
        IRI teamIri = context.modeller().team(
            orgIri,
            orgLogin + ":" + team.getSlug(),
            team.getName(),
            String.valueOf(team.getPrivacy()));

        try {
            for (GHUser member : team.getMembers()) {
                context.modeller().teamMember(teamIri, member.getLogin());
            }
        } catch (IOException e) {
            // best-effort member listing
        }
    }
}