package systems.symbol.trust;

import java.security.cert.X509Certificate;

public interface I_Authority {
public X509Certificate certify(I_Claim claim) throws BrokenTrust;

public I_Claim verify(X509Certificate claim) throws BrokenTrust;

public I_Sovereign authorize(X509Certificate cert) throws BrokenTrust;

public I_Sovereign trusts(I_Claim fair) throws BrokenTrust;
}