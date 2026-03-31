package systems.symbol.trust;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

public abstract class TrusteeKeys implements I_TrustKeys {
List<PrivateKey> guardians = new ArrayList<PrivateKey>();

public TrusteeKeys() {
}

public TrusteeKeys(PrivateKey sovereign) {
if (sovereign != null) {
this.guardians.add(sovereign);
this.majeur();
}
}

private List<PrivateKey> getKeys(int s, int l) {
if (this.guardians.isEmpty()) return List.of();
int end = Math.min(l, this.guardians.size());
int start = Math.min(s, end);
return new ArrayList<>(this.guardians.subList(start, end));
}

public List<PrivateKey> getGuardians() {
return this.getKeys(0, this.guardians.size());
}

public List<PrivateKey> getCustodians() {
return this.getKeys(3, this.guardians.size());
}

@Override
public PrivateKey getGovernanceKey() {
if (this.guardians.size() <= 4) return null;
return this.guardians.get(4);
}

@Override
public PrivateKey getContinuityKey() {
if (this.guardians.size() <= 3) return null;
return this.guardians.get(3);
}

@Override
public abstract void majeur();
}
