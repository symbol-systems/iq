package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;

import systems.symbol.connect.core.ConnectorGraphModeller;
import systems.symbol.connect.core.ConnectorModels;

/**
 * AWS graph modeller that maps discovered AWS resources into connector RDF state.
 */
public final class AwsModeller extends ConnectorGraphModeller {

    public AwsModeller(Model model, IRI graphIri, IRI ontologyBaseIri, IRI entityBaseIri) {
        super(model, graphIri, ontologyBaseIri, entityBaseIri);
    }

    public IRI account(IRI connectorId, String accountId, String arn) {
        IRI accountIri = entity("account", accountId);
        link(connectorId, ConnectorModels.HAS_ACCOUNT, accountIri);
        addLiteral(accountIri, "accountId", accountId);
        addLiteral(accountIri, "arn", arn);
        return accountIri;
    }

    public IRI region(IRI connectorId, String regionId, String endpoint) {
        IRI regionIri = entity("region", regionId);
        link(connectorId, ConnectorModels.HAS_REGION, regionIri);
        addLiteral(regionIri, "regionId", regionId);
        addLiteral(regionIri, "endpoint", endpoint);
        return regionIri;
    }

    public IRI s3Bucket(IRI connectorId, String bucketName) {
        IRI bucketIri = entity("s3", bucketName);
        link(connectorId, ConnectorModels.HAS_RESOURCE, bucketIri);
        addType(bucketIri, "S3Bucket");
        addLiteral(bucketIri, "name", bucketName);
        return bucketIri;
    }

    public IRI ec2Instance(IRI connectorId, String instanceId, String instanceType, String state) {
        IRI instanceIri = entity("ec2:instance", instanceId);
        link(connectorId, ConnectorModels.HAS_RESOURCE, instanceIri);
        addType(instanceIri, "EC2Instance");
        addLiteral(instanceIri, "instanceId", instanceId);
        addLiteral(instanceIri, "instanceType", instanceType);
        addLiteral(instanceIri, "state", state);
        return instanceIri;
    }

    public IRI iamUser(IRI connectorId, String userName, String arn) {
        IRI userIri = entity("iam:user", userName);
        link(connectorId, ConnectorModels.HAS_USER, userIri);
        addType(userIri, "IAMUser");
        addLiteral(userIri, "userName", userName);
        addLiteral(userIri, "arn", arn);
        return userIri;
    }

    public IRI iamRole(IRI connectorId, String roleName, String arn) {
        IRI roleIri = entity("iam:role", roleName);
        link(connectorId, ConnectorModels.HAS_ROLE, roleIri);
        addType(roleIri, "IAMRole");
        addLiteral(roleIri, "roleName", roleName);
        addLiteral(roleIri, "arn", arn);
        return roleIri;
    }

    public IRI iamGroup(IRI connectorId, String groupName) {
        IRI groupIri = entity("iam:group", groupName);
        link(connectorId, ConnectorModels.HAS_SUBSYSTEM, groupIri);
        addType(groupIri, "IAMGroup");
        addLiteral(groupIri, "groupName", groupName);
        return groupIri;
    }

    public IRI iamPolicy(IRI connectorId, String policyName, String arn) {
        IRI policyIri = entity("iam:policy", policyName);
        link(connectorId, ConnectorModels.HAS_POLICY, policyIri);
        addType(policyIri, "IAMPolicy");
        addLiteral(policyIri, "policyName", policyName);
        addLiteral(policyIri, "arn", arn);
        return policyIri;
    }

    public IRI cloudTrail(IRI connectorId, String trailName, String s3BucketName) {
        IRI trailIri = entity("cloudtrail:trail", trailName);
        link(connectorId, ConnectorModels.HAS_CONTROL, trailIri);
        addType(trailIri, "CloudTrailTrail");
        addLiteral(trailIri, "name", trailName);
        addLiteral(trailIri, "s3BucketName", s3BucketName);
        return trailIri;
    }

    public IRI configRecorder(IRI connectorId, String recorderName) {
        IRI recorderIri = entity("config:recorder", recorderName);
        link(connectorId, ConnectorModels.HAS_CONTROL, recorderIri);
        addType(recorderIri, "ConfigRecorder");
        addLiteral(recorderIri, "name", recorderName);
        return recorderIri;
    }

    public IRI configRule(IRI connectorId, String ruleName, String sourceOwner) {
        IRI ruleIri = entity("config:rule", ruleName);
        link(connectorId, ConnectorModels.HAS_CONTROL, ruleIri);
        addType(ruleIri, "ConfigRule");
        addLiteral(ruleIri, "name", ruleName);
        addLiteral(ruleIri, "sourceOwner", sourceOwner);
        return ruleIri;
    }

    public IRI pricingService(IRI connectorId, String serviceCode, String attributeNamesCsv) {
        IRI serviceIri = entity("pricing:service", serviceCode);
        link(connectorId, ConnectorModels.HAS_SUBSYSTEM, serviceIri);
        addType(serviceIri, "PricingService");
        addLiteral(serviceIri, "serviceCode", serviceCode);
        addLiteral(serviceIri, "attributeNames", attributeNamesCsv);
        return serviceIri;
    }
}
