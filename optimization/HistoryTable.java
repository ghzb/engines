package optimization;

import state.StateSpaceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HistoryTable {

    /**
     * The StateSpaceManager that houses the enums to use.
     */
    private StateSpaceManager manager;

    /**
     * The folder where "history.csv" will be exported to.
     */
    private final File saveFolder;

    /**
     * The history table. Used to store the decisions from an engine.
     */
    private final List<Result> history = new ArrayList<>();

    public HistoryTable(File saveFolder)
    {
        this.saveFolder = saveFolder;
    }

    public void add(Result result)
    {
        history.add(result);
    }

    /**
     * Write the current history to history.csv.
     */
    public void writeToFile()
    {
        try {
            FileWriter writer = new FileWriter(new File(saveFolder, "history.csv"));
            writer.write("State,Action,New State,Probability,Reward");

            for(Result result: history){
                String delimiter = ",";
                writer.write(System.lineSeparator());
                String row = result.oldState + delimiter +
                        result.actions + delimiter +
                        result.newState + delimiter +
                        result.probability + delimiter +
                        result.score + delimiter;
                writer.write(row);
            }

            writer.close();
        } catch (IOException err)
        {
            Exceptions.LOGGER.severe("Unable to write history", err);
        }
    }
}
