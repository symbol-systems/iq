package systems.symbol.trust;

public interface I_Claimed {
I_Claim getClaim();
I_Sovereign getSovereign() throws BrokenTrust;
}
