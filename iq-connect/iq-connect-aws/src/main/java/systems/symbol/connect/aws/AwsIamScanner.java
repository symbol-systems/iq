package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;

import systems.symbol.connect.core.ConnectorScanner;

import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.Group;
import software.amazon.awssdk.services.iam.model.Policy;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;

final class AwsIamScanner implements ConnectorScanner<AwsScanContext> {

    private final IamClient iam;

    AwsIamScanner(IamClient iam) {
        this.iam = iam;
    }

    @Override
    public void scan(AwsScanContext context) {
        AwsModeller modeller = context.modeller();
        IRI connectorId = context.connectorId();

        for (var usersPage : iam.listUsersPaginator()) {
            for (User user : usersPage.users()) {
                modeller.iamUser(connectorId, user.userName(), user.arn());
            }
        }

        for (var rolesPage : iam.listRolesPaginator()) {
            for (Role role : rolesPage.roles()) {
                modeller.iamRole(connectorId, role.roleName(), role.arn());
            }
        }

        for (var groupsPage : iam.listGroupsPaginator()) {
            for (Group group : groupsPage.groups()) {
                modeller.iamGroup(connectorId, group.groupName());
            }
        }

        for (var policiesPage : iam.listPoliciesPaginator()) {
            for (Policy policy : policiesPage.policies()) {
                modeller.iamPolicy(connectorId, policy.policyName(), policy.arn());
            }
        }
    }
}