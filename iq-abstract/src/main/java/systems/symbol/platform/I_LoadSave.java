package systems.symbol.platform;

import java.io.IOException;

/*
 *    Interface for I_LoadSave interface serves as a contract for components responsible for saving and loading IQ data,
 *    an operating environment tailored for symbolic cognition. IQ facilitates the transformation of RDF
 *    (Resource Description Framework) graphs into executable playbooks. Classes implementing this interface
 *    are tasked with handling the essential operations for persisting and retrieving data within the IQ environment.
 */
public interface I_LoadSave {

    /**
     * Saves the data of the operating environment.
     *
     * @throws IOException if an I/O error occurs while saving the data.
     */
    void save() throws IOException;

    /**
     * Loads the data into the operating environment.
     *
     * @throws IOException if an I/O error occurs while loading the data.
     */
    void load() throws IOException;

}
