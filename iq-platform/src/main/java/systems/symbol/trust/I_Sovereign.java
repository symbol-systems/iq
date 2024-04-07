package systems.symbol.trust;

import java.security.PrivateKey;

public interface I_Sovereign {

public I_Claim self() throws BrokenTrust;
public PrivateKey myKey() throws BrokenTrust;

}
