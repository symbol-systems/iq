package systems.symbol.trust.generate;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZoneOffset;
import java.util.Date;

public class CertGen {

ZoneOffset tz;
private static final String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withECDSA";

private String authorityCN;
private String signatureAlgorithm;
KeyPair authorityKeys;

public CertGen(KeyPair authorityKeys, String authorityCN, String signatureAlgorithm, ZoneOffset tz) {
this.authorityKeys = authorityKeys;
this.authorityCN = authorityCN;
this.signatureAlgorithm = signatureAlgorithm;
this.tz = tz;
}

public CertGen(KeyPair authorityKeys, String authorityCN) {
this(authorityKeys, authorityCN, DEFAULT_SIGNATURE_ALGORITHM, ZoneOffset.UTC);
}

public X509Certificate generate(KeyPair keypair, String subjectCN, Date startDate, Date expiryDate)
throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException,
SignatureException, InvalidKeyException, NoSuchProviderException {

SecureRandom random = new SecureRandom();
X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
.addRDN(BCStyle.CN, subjectCN)
.build();

BigInteger serial = new BigInteger(160, random);

X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
new X500Name(authorityCN),
serial,
startDate,
expiryDate,
subject,
keypair.getPublic());

JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();

SubjectKeyIdentifier subjectKeyId = extensionUtils.createSubjectKeyIdentifier(keypair.getPublic());

PublicKey authorityPublicKey = authorityKeys.getPublic();

AuthorityKeyIdentifier authorityKeyId = extensionUtils.createAuthorityKeyIdentifier(authorityPublicKey);

certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyId);
certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, authorityKeyId);
certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
certificateBuilder.addExtension(Extension.keyUsage, false,
new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature));
certificateBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(
new KeyPurposeId[] { KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth }));

ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
.build(authorityKeys.getPrivate());

X509Certificate cert = new JcaX509CertificateConverter()
.getCertificate(certificateBuilder.build(signer));

cert.verify(authorityKeys.getPublic());

return cert;
}
}
