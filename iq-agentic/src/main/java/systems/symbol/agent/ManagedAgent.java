package systems.symbol.agent;

import org.eclipse.rdf4j.model.*;
import org.jetbrains.annotations.NotNull;
import systems.symbol.decide.I_Decide;
import systems.symbol.fsm.StateException;
import systems.symbol.intent.I_Intent;

import javax.script.Bindings;

/**
 * The `Avatar` class represents an intelligent agent capable of interacting
 * with a large language model (LLM) to process and execute user intents.
 * This class acts as a mediator between the agent's internal
 * decision-making, the prompts and responses of the LLM.
 *
 * <ul>
 * <li>Manages agent's state machine and ensuring that state transitions
 * are performed according to the message intent.</li>
 * <li>Generating and processing prompts through the LLM, allowing the agent to
 * provide intelligent, context-aware responses.</li>
 * <li>Updating the conversation flow (`chat`) based on the LLM's output and
 * the agent's internal decision-making logic.</li>
 * <li>Heuristically determining the correct state transition based on user
 * intents, ensuring that the agent's behavior aligns with the user's
 * expectations.</li>
 * </ul>
 *
 * The intent flow in the Avatar class involves recognizing a user’s intent
 * through chat interactions, processing it using a LLM,
 * and then determining and executing the appropriate state transition within
 * the agent’s state machine.
 * The flow is designed to be modular, with clear separation of concerns between
 * intent recognition, state management, and interaction handling.
 */

public class ManagedAgent extends IntentAgent {
    I_Decide<Resource> manager;

    public ManagedAgent(I_Decide<Resource> manager, @NotNull IRI self, @NotNull Model ground, @NotNull Model thoughts,
            I_Intent intent,
            Bindings bindings) throws StateException {
        super(self, ground, thoughts, intent, bindings);
        this.manager = manager;
    }

}
