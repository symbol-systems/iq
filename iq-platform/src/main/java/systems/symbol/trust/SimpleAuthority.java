package systems.symbol.trust;

// import systems.symbol.acme.I_Issuer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.base.InternedIRI;

import javax.security.auth.x500.X500Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SimpleAuthority extends Authority {
// I_Issuer issuer = null;

public SimpleAuthority(I_Claim claim, PrivateKey key) throws BrokenTrust {
super(claim, key);
}

public SimpleAuthority(I_Claim claim, TrusteeKeys trustee) throws BrokenTrust {
super(claim, trustee);
}

/**
 * return a valid claim ...
 *
 * the certificate CN is used to locate
 * and return the persisted claim
 *
 * @param cert
 * @return valid claim
 */
protected I_Claim hydrate(X509Certificate cert) {
final I_Authority self = this;
X500Principal principal = cert.getSubjectX500Principal();
String issuer = cert.getIssuerX500Principal().getName();
return new I_Claim() {
 public I_Authority getAuthority() {
return self;
}

@Override
public String getName() {
return principal.getName();
}

@Override
public IRI getID() {
return new InternedIRI(issuer, getName());
}

@Override
public IRI getType() {
return new InternedIRI(issuer, getClass().getCanonicalName());
}
};
}

@Override
public I_Sovereign authorize(X509Certificate cert) throws BrokenTrust {
return null;
}

@Override
public X509Certificate certify(I_Claim claim) throws BrokenTrust {
// return issuer.issue(claim);
return null;
}

@Override
public I_Sovereign getSovereign() throws BrokenTrust {
return this.trusts(getClaim());
}
}
