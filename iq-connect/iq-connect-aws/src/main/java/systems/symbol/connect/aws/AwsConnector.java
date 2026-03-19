package systems.symbol.connect.aws;

import java.time.Instant;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Values;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

import systems.symbol.connect.core.Checkpoints;
import systems.symbol.connect.core.ConnectorMode;
import systems.symbol.connect.core.ConnectorStatus;
import systems.symbol.connect.core.ConnectorVocabulary;
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

    private volatile ConnectorStatus status = ConnectorStatus.IDLE;
    private volatile Optional<I_Checkpoint> checkpoint = Optional.empty();

    public AwsConnector(String connectorId, AwsConfig config, Model state) {
        this.connectorId = Values.iri(connectorId);
        this.config = config;
        this.state = state;
    }

    @Override
    public IRI getSelf() {
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
    public Model getStateModel() {
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

        Region region = Region.of(config.getRegion().orElse(DEFAULT_REGION));

        try (S3Client s3 = S3Client.builder().region(region).credentialsProvider(DefaultCredentialsProvider.create()).build()) {
            state.clear();

            state.add(connectorId, Values.iri(ConnectorVocabulary.SYNC_STATUS), Values.literal("SYNCING"));
            state.add(connectorId, Values.iri(ConnectorVocabulary.LAST_SYNCED_AT), Values.literal(Instant.now().toString()));

            for (Bucket bucket : s3.listBuckets().buckets()) {
                IRI bucketIri = Values.iri("urn:aws:s3:" + bucket.name());
                state.add(connectorId, Values.iri(ConnectorVocabulary.HAS_RESOURCE), bucketIri);
                state.add(bucketIri, Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), Values.iri("https://symbol.systems/ontology/aws#S3Bucket"));
                state.add(bucketIri, Values.iri("https://symbol.systems/ontology/aws#name"), Values.literal(bucket.name()));
            }

            checkpoint = Optional.of(Checkpoints.of(state));
            status = ConnectorStatus.IDLE;
        } catch (Exception e) {
            status = ConnectorStatus.ERROR;
            state.add(connectorId, Values.iri(ConnectorVocabulary.SYNC_STATUS), Values.literal("ERROR"));
            state.add(connectorId, Values.iri(ConnectorVocabulary.LAST_SYNCED_AT), Values.literal(Instant.now().toString()));
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
        m.add(connectorId, Values.iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), Values.iri("https://symbol.systems/ontology/connect#Connector"));
        m.add(connectorId, Values.iri("https://symbol.systems/ontology/connect#hasName"), Values.literal(getName()));
        m.add(connectorId, Values.iri("https://symbol.systems/ontology/connect#hasDescription"), Values.literal(getDescription()));
        return m;
    }
}
