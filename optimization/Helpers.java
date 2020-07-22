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

public class Helpers {
    protected transient static Logger LOGGER = Logger.getLogger(PythonOptimizationEngineOld.class);

    public static class VisitedStatesTracker
    {
        private final StateSpaceManager manager;
        private final int maxVisits;

        private Map<State, Integer> visitedStates;

        /**
         * Used to keep track of how many times each state is visited.
         * Provides method to increment entries and check if max threshold is met.
         * @param manager The manager that should be tracked.
         */
        public VisitedStatesTracker (StateSpaceManager manager, int maxVisits)
        {
            this.manager = manager;
            this.visitedStates = new HashMap<>();
            this.maxVisits = maxVisits;
        }
        /**
         * If a state is visited the maximum amount of times, the optimization engine will stop running.
         */
        public Boolean reachedMax()
        {
            int visitsFromMostVisitedState = Collections.max(visitedStates.values());
            return visitsFromMostVisitedState >= maxVisits;
        }

        public void reset()
        {
            visitedStates = new HashMap<>();
        }

        /**
         * TODO: documentation
         * Counts the amount of visits to each state.
         * @param state
         */
        public void add(State state)
        {
            visitedStates.put(state, visitedStates.getOrDefault(state, 0) + 1);
        }
    }

    public static class HistoryTable {

        /**
         * The StateSpaceManager that houses the enums to use.
         */
        private  StateSpaceManager manager;

        /**
         * The folder where "qtable.csv" will be exported to.
         */
        private final File saveFolder;

        /**
         * The Q-Table. Used to store state-action pairs.
         */
        private final List<StateActionCache.Result> history = new ArrayList<>();

        public HistoryTable(File saveFolder)
        {
            this.saveFolder = saveFolder;
        }

        public void add(StateActionCache.Result result)
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

                for(StateActionCache.Result result: history){
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
                LOGGER.severe("Unable to write history", err);
            }
        }
    }

    public static class QTable {

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

        public QTable(StateSpaceManager manager, File saveFolder)
        {
            this.manager = manager;
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
            LOGGER.fine(String.format("Creating zeroed-out Q-Table %d x %d", rows, columns));
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
                LOGGER.severe("Unable to write qtable", err);
            }

        }

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
    }

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
