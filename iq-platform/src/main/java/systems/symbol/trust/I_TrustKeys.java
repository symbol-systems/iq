package systems.symbol.trust;

import java.security.PrivateKey;

public interface I_TrustKeys {
 PrivateKey getGovernanceKey() throws BrokenTrust;
 PrivateKey getContinuityKey() throws BrokenTrust;
 void majeur() throws BrokenTrust;
}
