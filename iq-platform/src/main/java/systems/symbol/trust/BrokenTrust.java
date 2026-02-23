package systems.symbol.trust;
public class BrokenTrust extends Exception implements I_RiskException {
I_Claim self, risky;
public BrokenTrust(String msg, I_Claim self, I_Claim risky) {
super(msg);
this.self = self;
this.risky = risky;
}
@Override
public I_Claim getSelfClaim() {
return self;
}
@Override
public I_Claim getRiskyClaim() {
return risky;
}
}
