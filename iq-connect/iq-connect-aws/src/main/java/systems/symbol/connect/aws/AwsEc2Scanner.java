package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;

import systems.symbol.connect.core.ConnectorScanner;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

final class AwsEc2Scanner implements ConnectorScanner<AwsScanContext> {

    private final Ec2Client ec2;

    AwsEc2Scanner(Ec2Client ec2) {
        this.ec2 = ec2;
    }

    @Override
    public void scan(AwsScanContext context) {
        AwsModeller modeller = context.modeller();
        IRI connectorId = context.connectorId();
        IRI accountIri = context.accountIri();

        for (var instancePage : ec2.describeInstancesPaginator()) {
            for (Reservation reservation : instancePage.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String availabilityZone = instance.placement() != null ? instance.placement().availabilityZone() : null;
                    IRI instanceRegionIri = context.regionForAvailabilityZone(availabilityZone);

                    IRI instanceIri = modeller.ec2Instance(
                        connectorId,
                        accountIri,
                        instanceRegionIri,
                        instance.instanceId(),
                        instance.instanceTypeAsString(),
                        instance.state().nameAsString(),
                        availabilityZone);

                    if (instance.vpcId() != null && !instance.vpcId().isBlank()) {
                        IRI vpcIri = modeller.vpc(connectorId, accountIri, instanceRegionIri, instance.vpcId());
                        modeller.ec2InstanceInVpc(instanceIri, vpcIri);
                    }

                    if (instance.subnetId() != null && !instance.subnetId().isBlank()) {
                        IRI subnetIri = modeller.subnet(connectorId, accountIri, instanceRegionIri, instance.subnetId());
                        modeller.ec2InstanceInSubnet(instanceIri, subnetIri);
                    }

                    if (instance.securityGroups() != null) {
                        for (var securityGroup : instance.securityGroups()) {
                            if (securityGroup.groupId() == null || securityGroup.groupId().isBlank()) {
                                continue;
                            }
                            IRI securityGroupIri = modeller.securityGroup(connectorId, accountIri, instanceRegionIri, securityGroup.groupId());
                            modeller.ec2InstanceHasSecurityGroup(instanceIri, securityGroupIri);
                        }
                    }

                    if (instance.iamInstanceProfile() != null && instance.iamInstanceProfile().arn() != null && !instance.iamInstanceProfile().arn().isBlank()) {
                        IRI profileIri = modeller.iamInstanceProfile(connectorId, accountIri, instance.iamInstanceProfile().arn());
                        modeller.ec2InstanceUsesInstanceProfile(instanceIri, profileIri);
                    }
                }
            }
        }
    }
}