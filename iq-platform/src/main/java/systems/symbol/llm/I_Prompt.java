package systems.symbol.llm;

import systems.symbol.agent.tools.APIException;
import systems.symbol.fsm.StateException;

import java.io.IOException;

public interface I_Prompt<T> {

    /*
     * operate based on the provided prompt.
     * @param prompt The prompt message used to base the decision on.
     * @throws APIException If an API-related error occurs.
     * @throws IOException If an IO-related error occurs.
     * @throws StateException If an error occurs with the state machine.
     */
    I_Thread<T> prompt(String prompt) throws APIException, IOException, StateException;

    /*
     * operate based on the current prompt and historic context.
     * @param history An existing ChatThread for historic context.
     * @param prompt The prompt message to base the decision on.
     * @throws APIException If an API-related error occurs.
     * @throws IOException If an IO-related error occurs.
     * @throws StateException If an error occurs with the state machine.
     */
    I_Thread<T>  prompt(ChatThread history, String prompt) throws APIException, IOException, StateException;
}
