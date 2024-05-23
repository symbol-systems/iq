package systems.symbol;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.jetbrains.annotations.NotNull;
import systems.symbol.platform.TrustedPlatform;
import systems.symbol.rdf4j.io.RDFLoader;

/**
 * MY_IQ_AI: Neuro-symbolic AI Cognitive Partner.
 * <p>
 * IQ enforces operational governance for fleets of neuro-symbolic cognitive AI.
 * It converts RDF mind graphs into actionable MY.IQ.AI playbooks.
 */
public class MY_IQ_AI {

public static void main(String[] args) throws Exception {

if (args.length != 2) System.exit(0);

TrustedPlatform ai = getTrustedPlatform(args);
// Start MY.IQ.AI
ai.start(); // Start IQ.AI
ai.stop();  // Stop IQ.AI
ai.shutdown();  // Shutdown IQ.AI

Repository repository = ai.getRepository(args[0]); // Get repo for `true name`
try (RepositoryConnection connection = repository.getConnection()) {
RDFLoader loader = new RDFLoader(repository.getConnection());
//loader.load(ai.getSelf().stringValue(), new Fil);
}

}

@NotNull
private static TrustedPlatform getTrustedPlatform(String[] args) throws Exception {
TrustedPlatform ai = new TrustedPlatform(args[0]);

// Q = AI + IQ
ai.boot();  // Boot up AI
// Guardians of CODE - will be checked again during `start`
ai.X(); // Guard X
ai.XX();// Guard XX
ai.XXX();   // Guard XXX
ai.XXXX();  // Guard XXXX
// Oracle of TRUTH - Establish trust with Oracle
ai.trusts();
return ai;
}
}
