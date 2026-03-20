package systems.symbol.connect.aws;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;

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
addType(accountIri, "Account");
addLiteral(accountIri, "accountId", accountId);
addLiteral(accountIri, "arn", arn);
return accountIri;
}

public IRI region(IRI connectorId, String regionId, String endpoint) {
return region(connectorId, null, regionId, endpoint);
}

public IRI region(IRI connectorId, IRI accountIri, String regionId, String endpoint) {
IRI regionIri = entity("region", regionId);
link(connectorId, ConnectorModels.HAS_REGION, regionIri);
addType(regionIri, "Region");
addLiteral(regionIri, "regionId", regionId);
addLiteral(regionIri, "endpoint", endpoint);
linkAws(accountIri, "hasRegion", regionIri);
return regionIri;
}

public IRI s3Bucket(IRI connectorId, String bucketName) {
return s3Bucket(connectorId, null, null, bucketName);
}

public IRI s3Bucket(IRI connectorId, IRI accountIri, IRI regionIri, String bucketName) {
IRI bucketIri = entity("s3", bucketName);
link(connectorId, ConnectorModels.HAS_RESOURCE, bucketIri);
addType(bucketIri, "S3Bucket");
addLiteral(bucketIri, "name", bucketName);
linkAws(bucketIri, "inAccount", accountIri);
linkAws(bucketIri, "inRegion", regionIri);
linkAws(accountIri, "hasResource", bucketIri);
linkAws(regionIri, "hasResource", bucketIri);
return bucketIri;
}

public IRI ec2Instance(IRI connectorId, String instanceId, String instanceType, String state) {
return ec2Instance(connectorId, null, null, instanceId, instanceType, state, null);
}

public IRI ec2Instance(IRI connectorId,
   IRI accountIri,
   IRI regionIri,
   String instanceId,
   String instanceType,
   String state,
   String availabilityZone) {
IRI instanceIri = entity("ec2:instance", instanceId);
link(connectorId, ConnectorModels.HAS_RESOURCE, instanceIri);
addType(instanceIri, "EC2Instance");
addLiteral(instanceIri, "instanceId", instanceId);
addLiteral(instanceIri, "instanceType", instanceType);
addLiteral(instanceIri, "state", state);
addLiteral(instanceIri, "availabilityZone", availabilityZone);
linkAws(instanceIri, "inAccount", accountIri);
linkAws(instanceIri, "inRegion", regionIri);
linkAws(accountIri, "hasResource", instanceIri);
linkAws(regionIri, "hasResource", instanceIri);
return instanceIri;
}

public IRI vpc(IRI connectorId, IRI accountIri, IRI regionIri, String vpcId) {
IRI vpcIri = entity("ec2:vpc", vpcId);
link(connectorId, ConnectorModels.HAS_RESOURCE, vpcIri);
addType(vpcIri, "VPC");
addLiteral(vpcIri, "vpcId", vpcId);
linkAws(vpcIri, "inAccount", accountIri);
linkAws(vpcIri, "inRegion", regionIri);
return vpcIri;
}

public IRI subnet(IRI connectorId, IRI accountIri, IRI regionIri, String subnetId) {
IRI subnetIri = entity("ec2:subnet", subnetId);
link(connectorId, ConnectorModels.HAS_RESOURCE, subnetIri);
addType(subnetIri, "Subnet");
addLiteral(subnetIri, "subnetId", subnetId);
linkAws(subnetIri, "inAccount", accountIri);
linkAws(subnetIri, "inRegion", regionIri);
return subnetIri;
}

public IRI securityGroup(IRI connectorId, IRI accountIri, IRI regionIri, String securityGroupId) {
IRI sgIri = entity("ec2:security-group", securityGroupId);
link(connectorId, ConnectorModels.HAS_RESOURCE, sgIri);
addType(sgIri, "SecurityGroup");
addLiteral(sgIri, "securityGroupId", securityGroupId);
linkAws(sgIri, "inAccount", accountIri);
linkAws(sgIri, "inRegion", regionIri);
return sgIri;
}

public IRI iamInstanceProfile(IRI connectorId, IRI accountIri, String profileArn) {
IRI profileIri = entity("iam:instance-profile", profileArn);
link(connectorId, ConnectorModels.HAS_ROLE, profileIri);
addType(profileIri, "IAMInstanceProfile");
addLiteral(profileIri, "arn", profileArn);
linkAws(profileIri, "inAccount", accountIri);
return profileIri;
}

public void ec2InstanceInVpc(IRI instanceIri, IRI vpcIri) {
linkAws(instanceIri, "inVpc", vpcIri);
}

public void ec2InstanceInSubnet(IRI instanceIri, IRI subnetIri) {
linkAws(instanceIri, "inSubnet", subnetIri);
}

public void ec2InstanceHasSecurityGroup(IRI instanceIri, IRI securityGroupIri) {
linkAws(instanceIri, "hasSecurityGroup", securityGroupIri);
}

public void ec2InstanceUsesInstanceProfile(IRI instanceIri, IRI profileIri) {
linkAws(instanceIri, "usesInstanceProfile", profileIri);
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
return cloudTrail(connectorId, null, null, trailName, null, s3BucketName);
}

public IRI cloudTrail(IRI connectorId,
  IRI accountIri,
  IRI regionIri,
  String trailName,
  IRI s3BucketIri,
  String s3BucketName) {
IRI trailIri = entity("cloudtrail:trail", trailName);
link(connectorId, ConnectorModels.HAS_CONTROL, trailIri);
addType(trailIri, "CloudTrailTrail");
addLiteral(trailIri, "name", trailName);
addLiteral(trailIri, "s3BucketName", s3BucketName);
linkAws(trailIri, "inAccount", accountIri);
linkAws(trailIri, "inRegion", regionIri);
linkAws(trailIri, "logsToBucket", s3BucketIri);
return trailIri;
}

public IRI configRecorder(IRI connectorId, String recorderName) {
return configRecorder(connectorId, null, null, recorderName);
}

public IRI configRecorder(IRI connectorId, IRI accountIri, IRI regionIri, String recorderName) {
IRI recorderIri = entity("config:recorder", recorderName);
link(connectorId, ConnectorModels.HAS_CONTROL, recorderIri);
addType(recorderIri, "ConfigRecorder");
addLiteral(recorderIri, "name", recorderName);
linkAws(recorderIri, "inAccount", accountIri);
linkAws(recorderIri, "inRegion", regionIri);
return recorderIri;
}

public IRI configRule(IRI connectorId, String ruleName, String sourceOwner) {
return configRule(connectorId, null, null, null, ruleName, sourceOwner);
}

public IRI configRule(IRI connectorId,
  IRI accountIri,
  IRI regionIri,
  IRI recorderIri,
  String ruleName,
  String sourceOwner) {
IRI ruleIri = entity("config:rule", ruleName);
link(connectorId, ConnectorModels.HAS_CONTROL, ruleIri);
addType(ruleIri, "ConfigRule");
addLiteral(ruleIri, "name", ruleName);
linkAws(ruleIri, "inAccount", accountIri);
linkAws(ruleIri, "inRegion", regionIri);
linkAws(ruleIri, "managedByRecorder", recorderIri);

IRI sourceOwnerIri = configSourceOwner(sourceOwner);
linkAws(ruleIri, "hasSourceOwner", sourceOwnerIri);
return ruleIri;
}

public IRI pricingService(IRI connectorId, String serviceCode, Iterable<String> attributeNames) {
IRI serviceIri = entity("pricing:service", serviceCode);
link(connectorId, ConnectorModels.HAS_SUBSYSTEM, serviceIri);
addType(serviceIri, "PricingService");
addLiteral(serviceIri, "serviceCode", serviceCode);
if (attributeNames != null) {
for (String attributeName : attributeNames) {
IRI attributeIri = pricingAttribute(serviceCode, attributeName);
linkAws(serviceIri, "hasAttribute", attributeIri);
}
}
return serviceIri;
}

private IRI pricingAttribute(String serviceCode, String attributeName) {
IRI attributeIri = entity("pricing:attribute", serviceCode + ":" + attributeName);
addType(attributeIri, "PricingAttribute");
addLiteral(attributeIri, "name", attributeName);
return attributeIri;
}

private IRI configSourceOwner(String sourceOwner) {
if (sourceOwner == null || sourceOwner.isBlank()) {
return null;
}
IRI ownerIri = entity("config:source-owner", sourceOwner);
addType(ownerIri, "ConfigSourceOwner");
addLiteral(ownerIri, "name", sourceOwner);
return ownerIri;
}

private void linkAws(Resource subject, String predicateLocalName, Resource object) {
if (subject == null || object == null) {
return;
}
add(subject, ontology(predicateLocalName), object);
}
}
