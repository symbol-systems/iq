package systems.symbol.control.election;

import java.time.Instant;

/**
 * Result of a leader election attempt.
 */
public record LeaderElectionResult(
boolean elected,   // true if this node became leader
String leaderId,// current leader's nodeId (may not be this node)
Instant electionTime,   // when the election occurred
String reason   // explanation (e.g., "elected as new leader", "leader already exists")
) {
public static LeaderElectionResult alreadyLeader(String leaderId, Instant time) {
return new LeaderElectionResult(false, leaderId, time, "Already have an active leader");
}

public static LeaderElectionResult elected(String leaderId, Instant time) {
return new LeaderElectionResult(true, leaderId, time, "Elected as new leader");
}

public static LeaderElectionResult failed(String leaderId, Instant time, String reason) {
return new LeaderElectionResult(false, leaderId, time, reason);
}
}
