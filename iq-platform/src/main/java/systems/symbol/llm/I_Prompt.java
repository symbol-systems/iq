package systems.symbol.llm;

import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;

import java.io.IOException;

public interface I_Prompt<T> {

//    /*
//     * operate based on the provided prompt.
//     * @param history The prompt messages used to base decision on.
//     * @throws APIException If an API-related error occurs.
//     * @throws IOException If an IO-related error occurs.
//     * @throws StateException If an error occurs with the state machine.
//     */
    I_Thread<T>  prompt(I_Thread<T> history) throws APIException, IOException, StateException;
}
