package optimization;

import com.sun.istack.logging.Logger;
import state.ActionEnumeration;
import state.State;
import state.StateEnumeration;
import state.StateSpaceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Exceptions {
    protected transient static Logger LOGGER = Logger.getLogger(PythonOptimizationEngineOld.class);

    /**
     * An error that occurs when results are accessed before the algorithm runs.
     */
    public static class PrematureInvocationException extends NullPointerException
    {
        @Override
        public String getMessage()
        {
            LOGGER.severe("getting results before policy generated");
            return "The optimal policy has not been created yet";
        }
    }
}
