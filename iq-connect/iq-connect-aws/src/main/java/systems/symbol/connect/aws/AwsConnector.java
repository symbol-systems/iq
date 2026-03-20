package systems.symbol.connect.aws;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.Modeller;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.Trail;
import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.config.model.ConfigurationRecorder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.Group;
import software.amazon.awssdk.services.iam.model.Policy;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.iam.model.User;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.DescribeServicesRequest;
import software.amazon.awssdk.services.pricing.model.DescribeServicesResponse;
import software.amazon.awssdk.services.pricing.model.Service;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import systems.symbol.connect.core.AbstractConnector;
import systems.symbol.connect.core.ConnectorMode;

/**
 * Example AWS connector implementation.
 *
 * <p>This connector is intended as a complete implementation showing how to
 * integrate AWS SDK calls with the IQ connector model and writes metadata into
 * the connector state model.
 */
public final class AwsConnector extends AbstractConnector {

    private static final String DEFAULT_REGION = "us-east-1";

    private final AwsConfig config;

    public AwsConnector(String connectorId, AwsConfig config, Model state) {
        this(connectorId,
            config,
            state,
            Values.iri(connectorId + "/graph/current"),
            Values.iri(Modeller.getAwsOntology()),
            Values.iri("urn:aws:"));
    }

    public AwsConnector(String connectorId, AwsConfig config, Model state, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
        super(connectorId, state, graphIri, ontologyBaseIri, entityBaseIri);
        this.config = config;
    }

    @Override
    public ConnectorMode getMode() {
        return ConnectorMode.READ_WRITE;
    }

    @Override
    protected void doRefresh() {
        IRI connectorId = getConnectorId();
        Region region = Region.of(config.getRegion().orElse(DEFAULT_REGION));
        AwsModeller modeller = new AwsModeller(getModel(), graphIri(), ontologyBaseIri(), entityBaseIri());

        try (S3Client s3 = S3Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             StsClient sts = StsClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             Ec2Client ec2 = Ec2Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             IamClient iam = IamClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             CloudTrailClient cloudTrail = CloudTrailClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             ConfigClient configClient = ConfigClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             PricingClient pricing = PricingClient.builder().region(Region.US_EAST_1).credentialsProvider(DefaultCredentialsProvider.create()).build()) {

            // Account identity
            GetCallerIdentityResponse identity = sts.getCallerIdentity();
            IRI accountIri = modeller.account(connectorId, identity.account(), identity.arn());

            Map<String, IRI> regionIrisById = new HashMap<>();
            Map<String, IRI> bucketIrisByName = new HashMap<>();

            // Regions: account -> zone discovery (configured + active)
            ensureRegion(modeller, regionIrisById, accountIri, region.id(), null);

            DescribeRegionsResponse regionsResponse = ec2.describeRegions();
            for (software.amazon.awssdk.services.ec2.model.Region awsRegion : regionsResponse.regions()) {
                ensureRegion(modeller, regionIrisById, accountIri, awsRegion.regionName(), awsRegion.endpoint());
            }

            // S3 buckets
            for (Bucket bucket : s3.listBuckets().buckets()) {
                IRI bucketIri = modeller.s3Bucket(connectorId, accountIri, null, bucket.name());
                bucketIrisByName.put(bucket.name(), bucketIri);
            }

            // EC2 instances
            for (var instancePage : ec2.describeInstancesPaginator()) {
                for (Reservation reservation : instancePage.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String availabilityZone = instance.placement() != null ? instance.placement().availabilityZone() : null;
                    String instanceRegionId = regionFromAvailabilityZone(availabilityZone).orElse(region.id());
                    IRI instanceRegionIri = ensureRegion(modeller, regionIrisById, accountIri, instanceRegionId, null);

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

            // IAM users, roles, groups, policies (all pages)
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

            // CloudTrail trails, linked to bucket and region
            var trailsResponse = cloudTrail.describeTrails(
                software.amazon.awssdk.services.cloudtrail.model.DescribeTrailsRequest.builder()
                    .includeShadowTrails(true)
                    .build());
            for (Trail trail : trailsResponse.trailList()) {
                IRI trailRegionIri = ensureRegion(modeller, regionIrisById, accountIri, trail.homeRegion(), null);
                IRI trailBucketIri = ensureBucket(modeller, bucketIrisByName, accountIri, trail.s3BucketName());
                modeller.cloudTrail(connectorId, accountIri, trailRegionIri, trail.name(), trailBucketIri, trail.s3BucketName());
            }

            // Config recorders
            IRI configRecorderIri = null;
            for (ConfigurationRecorder recorder : configClient.describeConfigurationRecorders().configurationRecorders()) {
                IRI discoveredRecorderIri = modeller.configRecorder(connectorId, accountIri, ensureRegion(modeller, regionIrisById, accountIri, region.id(), null), recorder.name());
                if (configRecorderIri == null) {
                    configRecorderIri = discoveredRecorderIri;
                } else {
                    configRecorderIri = null;
                }
            }

            // Config rules (all pages)
            String configRulesNextToken = null;
            do {
                var request = software.amazon.awssdk.services.config.model.DescribeConfigRulesRequest.builder();
                if (configRulesNextToken != null && !configRulesNextToken.isBlank()) {
                    request.nextToken(configRulesNextToken);
                }

                var configRules = configClient.describeConfigRules(request.build());
                for (software.amazon.awssdk.services.config.model.ConfigRule rule : configRules.configRules()) {
                    String sourceOwner = rule.source() != null ? rule.source().ownerAsString() : null;
                    modeller.configRule(connectorId,
                        accountIri,
                        ensureRegion(modeller, regionIrisById, accountIri, region.id(), null),
                        configRecorderIri,
                        rule.configRuleName(),
                        sourceOwner);
                }
                configRulesNextToken = configRules.nextToken();
            } while (configRulesNextToken != null && !configRulesNextToken.isBlank());

            // Pricing service catalog (all pages)
            String pricingNextToken = null;
            do {
                DescribeServicesRequest.Builder request = DescribeServicesRequest.builder();
                if (pricingNextToken != null && !pricingNextToken.isBlank()) {
                    request.nextToken(pricingNextToken);
                }

                DescribeServicesResponse pricingServicesResponse = pricing.describeServices(request.build());
                for (Service pricingService : pricingServicesResponse.services()) {
                    modeller.pricingService(connectorId, pricingService.serviceCode(), pricingService.attributeNames());
                }
                pricingNextToken = pricingServicesResponse.nextToken();
            } while (pricingNextToken != null && !pricingNextToken.isBlank());
        }
    }

    private IRI ensureRegion(AwsModeller modeller,
                             Map<String, IRI> regionIrisById,
                             IRI accountIri,
                             String regionId,
                             String endpoint) {
        if (regionId == null || regionId.isBlank()) {
            return null;
        }
        return regionIrisById.computeIfAbsent(regionId, key -> modeller.region(getConnectorId(), accountIri, key, endpoint));
    }

    private IRI ensureBucket(AwsModeller modeller,
                             Map<String, IRI> bucketIrisByName,
                             IRI accountIri,
                             String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            return null;
        }
        return bucketIrisByName.computeIfAbsent(bucketName, key -> modeller.s3Bucket(getConnectorId(), accountIri, null, key));
    }

    private Optional<String> regionFromAvailabilityZone(String availabilityZone) {
        if (availabilityZone == null || availabilityZone.isBlank()) {
            return Optional.empty();
        }

        char last = availabilityZone.charAt(availabilityZone.length() - 1);
        if (Character.isLetter(last) && availabilityZone.length() > 1) {
            return Optional.of(availabilityZone.substring(0, availabilityZone.length() - 1));
        }
        return Optional.of(availabilityZone);
    }
}
