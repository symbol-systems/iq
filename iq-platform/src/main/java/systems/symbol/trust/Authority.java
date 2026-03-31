package systems.symbol.trust;

import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;

public abstract class Authority implements I_Authority, I_TrustKeys, I_Claimed {
    TrusteeKeys trustee;
    I_Claim self;

    /**
     * The key and the claim will be bound together
     * In effect, we claim the authority is self-sovereign
     *
     * @param claim
     * @param key
     * @throws BrokenTrust
     */
    public Authority(I_Claim claim, PrivateKey key) throws BrokenTrust {
        this(claim, new TrusteeKeys(key) {
            @Override
            public void majeur() {
                // Sovereign key is added during TrusteeKeys construction
            }
        });
        this.self(); // zero-trust
    }

    /**
     * Act as proxy Authority for provided TrustChain
     * @param claim
     * @param trustee
     * @throws BrokenTrust
     */
    public Authority(I_Claim claim, TrusteeKeys trustee) throws BrokenTrust {
        this.self = claim;
        this.trustee = trustee;
        this.self(); // zero-trust
    }

    @Override
    public void majeur() throws BrokenTrust {
        this.self(); // zero-trust
    }

    @Override
    public X509Certificate certify(I_Claim claim) throws BrokenTrust {
        return null;
    }

    @Override
    public I_Claim verify(X509Certificate cert) {
        return this.hydrate(cert);
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
    protected abstract I_Claim hydrate(X509Certificate cert);

    @Override
    public abstract I_Sovereign authorize(X509Certificate cert) throws BrokenTrust;


    public I_Sovereign self() throws BrokenTrust {
        return trusts(this.getClaim());
    }

    /**
     * Strategy to determine trusted equivalence
     * @param claim
     * @param verify
     * @throws BrokenTrust
     */
    public void valid(I_Claim claim, I_Claim verify) throws BrokenTrust {
        if (verify.getType() != claim.getType())
            throw new BrokenTrust("invalid/type", this.getClaim(), claim);
        if (verify.getID() != claim.getID())
            throw new BrokenTrust("invalid/id", this.getClaim(), claim);
        if (verify.getAuthority() != claim.getAuthority())
            throw new BrokenTrust("invalid/authority", this.getClaim(), claim);
    }

    /**
     * If a claim is fully trusted - it is a sovereign claim
     * Re-certify: attempt to certify the claim
     * verify: attempt to re-hydrate the claim
     * valid: compare verified and fair claim as same
     * @param fair
     * @return
     * @throws BrokenTrust
     */
    public I_Sovereign trusts(I_Claim fair) throws BrokenTrust {
        // zero-trust: so double check
        X509Certificate certify = this.certify(fair);
        if (certify == null) {
            throw new BrokenTrust("x509/uncertified", this.getClaim(), fair);
        }
        I_Claim verify = this.verify(certify);

        // don't trust the cert
        try {
            certify.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new BrokenTrust("x509/expired", this.getClaim(), fair);
        } catch (CertificateNotYetValidException e) {
            throw new BrokenTrust("x509/not-yet", this.getClaim(), fair);
        }

        // if both claims are equivalent, no drama ...
        this.valid(fair, verify);
        // ** trust inferred **
        return this.authorize(certify); // and conferred
    }

    @Override
    public I_Claim getClaim() {
        return this.self;
    }

    public List<PrivateKey> getGuardians() throws BrokenTrust {
        return trustee.getGuardians();
    }

    public List<PrivateKey> getCustodians() throws BrokenTrust {
        return trustee.getCustodians();
    }

    @Override
    public PrivateKey getGovernanceKey() throws BrokenTrust {
        return trustee.getGovernanceKey();
    }

    @Override
    public PrivateKey getContinuityKey() throws BrokenTrust {
        return trustee.getContinuityKey();
    }
}
