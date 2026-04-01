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
 * It converts RDF fact graphs into actionable MY.IQ.AI playbooks.
 */
public class MY_IQ_AI {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) System.exit(0);

        TrustedPlatform ai = getTrustedPlatform(args);
        // Start MY.IQ.AI
        ai.start();     // Start IQ.AI
        ai.stop();      // Stop IQ.AI
        ai.shutdown();  // Shutdown IQ.AI

        Repository repository = ai.getRepository(args[0]); // Get repo for `true name`
        try (RepositoryConnection connection = repository.getConnection()) {
            RDFLoader loader = new RDFLoader(repository.getConnection());
//            loader.load(ai.getSelf().stringValue(), new Fil);
        }

    }

    @NotNull
    private static TrustedPlatform getTrustedPlatform(String[] args) throws Exception {
        TrustedPlatform ai = new TrustedPlatform(args[0]);

        // Q = AI + IQ
        ai.boot();  // Boot up AI
        // Trust gates — verified again during `start`
        ai.verifyIdentity();     // Verify platform identity
        ai.verifyCrypto();       // Verify cryptographic material
        ai.verifyEnvironment();  // Verify runtime environment
        ai.verifyTrustChain();   // Verify external trust chain
        // Oracle of TRUTH - Establish trust with Oracle
        ai.trusts();
        return ai;
    }
}
