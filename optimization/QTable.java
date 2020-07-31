package optimization;

import state.ActionEnumeration;
import state.StateEnumeration;
import state.StateSpaceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class QTable {

    /**
     * The StateSpaceManager that houses the enums to use.
     */
    private final StateSpaceManager manager;

    /**
     * The folder where "qtable.csv" will be exported to.
     */
    private final File saveFolder;

    /**
     * The Q-Table. Used to store state-action pairs.
     */
    private final Map<Integer, Map<Integer, Double>> qTable;

    public QTable(File saveFolder)
    {
        this.manager = StateSpaceManager.getManager(saveFolder);
        this.saveFolder = saveFolder;
        this.qTable = this.createQTable(manager.getNumberOfStates(), manager.getNumberOfActions());
    }

    /**
     * Create a zeroed-out table with specific dimensions.
     * @param rows The number of rows.
     * @param columns The number of columns.
     * @return A new HashMap.
     */
    private Map<Integer, Map<Integer, Double>> createQTable(int rows, int columns) {
        Exceptions.LOGGER.fine(String.format("Creating zeroed-out Q-Table %d x %d", rows, columns));
        Map<Integer, Map<Integer, Double>> table = new HashMap<>();
        for (int i=0; i < rows; i++) {
            HashMap<Integer, Double> row = new HashMap<>();
            for (int j=0; j < columns; j++) {
                row.put(j, 0d);
            }
            table.put(i, row);
        }
        return table;
    }

    /**
     * Add an item to the Q-Table.
     * @param stateIndex - The index of the state in the state space manager.
     * @param actionIndex - The index of the action in the state space manager.
     * @param score - The score/reward for the given state/action pair.
     */
    public void put(int stateIndex, int actionIndex, double score)
    {
        qTable.get(stateIndex).put(actionIndex, score);
    }

    /**
     * Write the current qTable to qtable.csv.
     */
    public void writeToFile()
    {
        try {

            int i = 0;
            List<StateEnumeration> stateOrder =  manager.getStateEnumOrder();
            List<List<String>> possibleStates = new ArrayList<>();
            for (;i < stateOrder.size(); i++)
            {
                Enum<?> e = (Enum<?>)stateOrder.get(i);
                Class<?> eClass = e.getDeclaringClass();
                List<String> space = Arrays.stream(eClass.getEnumConstants()).map(String::valueOf).collect(Collectors.toList());
                possibleStates.add(space);
            }

            List<List<String>> stateNames = Cartesian.productFrom(possibleStates);


            List<ActionEnumeration> actionOrder =  manager.getPossibleActions();
            List<List<String>> possibleActions = new ArrayList<>();
            for (;i - stateOrder.size() < actionOrder.size(); i++)
            {
                Enum<?> e = (Enum<?>)actionOrder.get(i - stateOrder.size());
                Class<?> eClass = e.getDeclaringClass();
                List<String> space = Arrays.stream(eClass.getEnumConstants()).map(String::valueOf).collect(Collectors.toList());
                possibleActions.add(space);
            }

            List<List<String>> actionNames = Cartesian.productFrom(possibleActions);


            FileWriter writer = new FileWriter(new File(saveFolder, "qtable.csv"));
            writer.write("State/Action,");
            for (int actionId = 0; actionId < manager.getNumberOfActions(); actionId++)
            {
                String colHeading = String.join("/", actionNames.get(actionId));
                writer.write(String.format("%s,", colHeading));
            }
            writer.write(System.lineSeparator());
            for (int stateId = 0; stateId < manager.getNumberOfStates(); stateId ++) {
                String rowHeading = String.join("/", stateNames.get(stateId));
                writer.write(String.format("%s,", rowHeading));
                for (int actionId = 0; actionId < manager.getNumberOfActions(); actionId++)
                {
                    writer.write(String.format("%f,", qTable.get(stateId).get(actionId)));
                }
                writer.write(System.lineSeparator());
            }
            writer.close();
        } catch (IOException err)
        {
            Exceptions.LOGGER.severe("Unable to write qtable", err);
        }

    }

    /**
     * Collect the actions with the highest reward for each state.
     * @return the best action for each state.
     */
    public Map<Integer, Map<Integer, Double>> getOptimalPolicy()
    {
        Map<Integer, Map<Integer, Double>> optimalPolicy = new HashMap<>();
        for(int i=0; i<qTable.size(); i++)
        {

            int bestActionId = Collections.max(qTable.get(i).entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();

            Double bestActionValue = qTable.get(i).get(bestActionId);
            optimalPolicy.put(i, Map.ofEntries(Map.entry(bestActionId, bestActionValue)));
        }
        return optimalPolicy;
    }

    public Map<Integer, Double> getActions(int stateIndex)
    {
        return qTable.get(stateIndex);
    }
}
