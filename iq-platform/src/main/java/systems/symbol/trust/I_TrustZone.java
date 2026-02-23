package systems.symbol.trust;

import java.security.PrivateKey;

public interface I_TrustZone {

    public I_Sovereign getSovereign();

    public void unlock(I_TrustKeys trustee, PrivateKey sovereign)  throws BrokenTrust;

    public void accept(I_Claim claim) throws BrokenTrust;
    public void revoke(I_Claim claim) throws BrokenTrust;
    public void forget(I_Claim claim) throws BrokenTrust;

    public boolean valid(I_Claim claim) throws BrokenTrust;
}
