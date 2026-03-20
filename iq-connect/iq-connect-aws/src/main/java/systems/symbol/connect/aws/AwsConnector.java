package systems.symbol.connect.aws;

import java.time.Instant;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import systems.symbol.connect.core.Modeller;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.Trail;
import software.amazon.awssdk.services.config.ConfigClient;
import software.amazon.awssdk.services.config.model.ConfigurationRecorder;
import software.amazon.awssdk.services.config.model.DescribeConfigRulesResponse;
import software.amazon.awssdk.services.config.model.ConfigRule;
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

import systems.symbol.connect.core.Checkpoints;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorProvenance;
import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.ConnectorSyncMetadata;
import systems.symbol.connect.core.I_Checkpoint;
import systems.symbol.connect.core.I_Connector;
import systems.symbol.connect.core.I_ConnectorDescriptor;

/**
 * Example AWS connector implementation.
 *
 * <p>This connector is intended as a complete implementation showing how to
 * integrate AWS SDK calls with the IQ connector model. It performs a simple
 * scan of all AWS resource and writes metadata into the connector state model.
 */
public final class AwsConnector implements I_Connector, I_ConnectorDescriptor {

    private static final String DEFAULT_REGION = "us-east-1";

    private final IRI connectorId;
    private final Model state;
    private final AwsConfig config;
    private final IRI graphIri;
    private final IRI ontologyBaseIri;
    private final IRI entityBaseIri;

    private volatile ConnectorStatus status = ConnectorStatus.IDLE;
    private volatile Optional<I_Checkpoint> checkpoint = Optional.empty();

    public AwsConnector(String connectorId, AwsConfig config, Model state) {
        this(connectorId,
            config,
            state,
            Values.iri(connectorId + "/graph/current"),
            Values.iri(Modeller.getAwsOntology()),
            Values.iri("urn:aws:"));
    }

    public AwsConnector(String connectorId, AwsConfig config, Model state, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
        this.connectorId = Values.iri(connectorId);
        this.config = config;
        this.state = state;
        this.graphIri = graphIri;
        this.ontologyBaseIri = ontologyBaseIri;
        this.entityBaseIri = entityBaseIri;
    }

    @Override
    public IRI getSelf() {
        return connectorId;
    }

    @Override
    public IRI getConnectorId() {
        return connectorId;
    }

    @Override
    public ConnectorMode getMode() {
        return ConnectorMode.READ_WRITE;
    }

    @Override
    public ConnectorStatus getStatus() {
        return status;
    }

    @Override
    public Model getModel() {
        return state;
    }

    @Override
    public Optional<I_Checkpoint> getCheckpoint() {
        return checkpoint;
    }

    @Override
    public void start() {
        status = ConnectorStatus.SYNCING;
    }

    @Override
    public void stop() {
        status = ConnectorStatus.IDLE;
    }

    @Override
    public void refresh() {
        status = ConnectorStatus.SYNCING;
        state.remove(null, null, null, graphIri);
        ConnectorSyncMetadata.markSyncing(state, connectorId, graphIri);

        IRI activity = ConnectorProvenance.markSyncStarted(state, connectorId, graphIri);
        Region region = Region.of(config.getRegion().orElse(DEFAULT_REGION));
        AwsModeller modeller = new AwsModeller(state, graphIri, ontologyBaseIri, entityBaseIri);

        try (S3Client s3 = S3Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             StsClient sts = StsClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             Ec2Client ec2 = Ec2Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             IamClient iam = IamClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             CloudTrailClient cloudTrail = CloudTrailClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             ConfigClient configClient = ConfigClient.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build();
             PricingClient pricing = PricingClient.builder().region(Region.US_EAST_1).credentialsProvider(DefaultCredentialsProvider.create()).build()) {

            // Account identity
            GetCallerIdentityResponse identity = sts.getCallerIdentity();
            modeller.account(connectorId, identity.account(), identity.arn());

            // Regions: account -> zone discovery (configured + active)
            modeller.region(connectorId, region.id(), null);

            DescribeRegionsResponse regionsResponse = ec2.describeRegions();
            for (software.amazon.awssdk.services.ec2.model.Region awsRegion : regionsResponse.regions()) {
                modeller.region(connectorId, awsRegion.regionName(), awsRegion.endpoint());
            }

            // S3 buckets
            for (Bucket bucket : s3.listBuckets().buckets()) {
                modeller.s3Bucket(connectorId, bucket.name());
            }

            // EC2 instances
            for (Reservation reservation : ec2.describeInstances().reservations()) {
                for (Instance instance : reservation.instances()) {
                    modeller.ec2Instance(
                        connectorId,
                        instance.instanceId(),
                        instance.instanceTypeAsString(),
                        instance.state().nameAsString());
                }
            }

            // IAM users, roles, groups, policies
            for (User user : iam.listUsers().users()) {
                modeller.iamUser(connectorId, user.userName(), user.arn());
            }

            for (Role role : iam.listRoles().roles()) {
                modeller.iamRole(connectorId, role.roleName(), role.arn());
            }

            for (Group group : iam.listGroups().groups()) {
                modeller.iamGroup(connectorId, group.groupName());
            }

            for (Policy policy : iam.listPolicies().policies()) {
                modeller.iamPolicy(connectorId, policy.policyName(), policy.arn());
            }

            // CloudTrail trails
            for (Trail trail : cloudTrail.describeTrails().trailList()) {
                modeller.cloudTrail(connectorId, trail.name(), trail.s3BucketName());
            }

            // Config rules and recorders
            for (ConfigurationRecorder recorder : configClient.describeConfigurationRecorders().configurationRecorders()) {
                modeller.configRecorder(connectorId, recorder.name());
            }

            DescribeConfigRulesResponse configRules = configClient.describeConfigRules();
            for (software.amazon.awssdk.services.config.model.ConfigRule rule : configRules.configRules()) {
                modeller.configRule(connectorId, rule.configRuleName(), rule.source().ownerAsString());
            }

            // Pricing service catalog snapshot (limited to first page)
            DescribeServicesResponse pricingServicesResponse = pricing.describeServices(DescribeServicesRequest.builder().maxResults(50).build());
            for (Service pricingService : pricingServicesResponse.services()) {
                modeller.pricingService(connectorId, pricingService.serviceCode(), String.join(",", pricingService.attributeNames()));
            }

            checkpoint = Optional.of(Checkpoints.of(state));
            status = ConnectorStatus.IDLE;
            ConnectorSyncMetadata.markSynced(state, connectorId, graphIri);
            ConnectorProvenance.markSyncCompleted(state, activity, graphIri);
        } catch (Exception e) {
            status = ConnectorStatus.ERROR;
            ConnectorSyncMetadata.markError(state, connectorId, graphIri);
            ConnectorProvenance.markSyncFailed(state, activity, e, graphIri);
        }
    }

    // I_ConnectorDescriptor implementation

    @Override
    public String getName() {
        return "AWS Connector";
    }

    @Override
    public String getDescription() {
        return "Syncs AWS account state into IQ.";
    }

    @Override
    public Model getDescriptorModel() {
        Model m = new LinkedHashModel();
        m.add(connectorId, Modeller.rdfType(), Modeller.connect("Connector"));
        m.add(connectorId, Modeller.connect("hasName"), Values.literal(getName()));
        m.add(connectorId, Modeller.connect("hasDescription"), Values.literal(getDescription()));
        return m;
    }
}
