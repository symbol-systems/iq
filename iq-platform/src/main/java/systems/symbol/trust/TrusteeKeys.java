package systems.symbol.trust;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

public abstract class TrusteeKeys implements I_TrustKeys {
    List<PrivateKey> guardians = new ArrayList<PrivateKey>();

    public TrusteeKeys() {
    }

    public TrusteeKeys(PrivateKey sovereign) {
        if (sovereign != null)
            this.majeur();
    }

    private List<PrivateKey> getKeys(int s, int l) {
        List<PrivateKey> keys = new ArrayList<>();
        if (l < 0 || l > keys.size())
            l = keys.size();
        for (int i = s; i < l; i++) {
            keys.add(this.guardians.get(i));
        }
        return keys;
    }

    public List<PrivateKey> getGuardians() {
        return this.getKeys(0, this.guardians.size());
    }

    public List<PrivateKey> getCustodians() {
        return this.getKeys(3, this.guardians.size());
    }

    @Override
    public PrivateKey getGovernanceKey() {
        return this.guardians.get(4);
    }

    @Override
    public PrivateKey getContinuityKey() {
        return this.guardians.get(3);
    }

    @Override
    public abstract void majeur();
}
