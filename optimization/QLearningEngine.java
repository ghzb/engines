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

/**
 *
 * @author Zeb Burroughs
 */
public class QLearningEngine implements OptimizationEngine {

    protected transient static Logger LOGGER = Logger.getLogger(ValueIterationEngine.class);

    /**
     * The rate at which the algorithm explores.
     * Exploring means a random action is selected rather than choosing the best action.
     * <ul>
     *     <li> Setting closer to 0 means Q-Values are rarely explores.
     *     <li> Setting closer to 1 means Q-Values are frequently explores.
     * </ul>
     */
    private double epsilon = 0.1;

    /**
     * The rate at which the algorithm learns.
     * <ul>
     *     <li> Setting closer to 0 means Q-Values are rarely updated.
     *     <li> Setting closer to 1 means Q-Values are frequently updated.
     * </ul>
     */
    private double alpha = 0.1;

    /**
     * The rate at which rewards diminish.
     * Overtime, rewards my not be as valuable.
     * <ul>
     *     <li> Setting closer to 0 means the algorithm think about the preset.
     *     <li> Setting closer to 1 means the algorithm think about the future.
     * </ul>
     */
    private double gamma = 0.9;

    /**
     * The index of the current state.
     */
    private int currentStateId;

    /**
     * The index of the current action.
     */
    private int currentActionId = 0;

    /**
     * The number of times the algorithm will try to reach the terminal state after resetting.
     */
    private int episodeThreshold;

    /**
     * The number of times the algorithm will try to reach the terminal state before resetting.
     */
    private int stepThreshold;

    /**
     * The current episode.
     */
    private int currentEpisode = 0;

    /**
     * The current step.
     */
    private int currentStep = 0;

    /**
     * If the algorithm should stop iterating.
     */
    private boolean isForceQuitting = false;

    /**
     * The location of where data is saved.
     */
    private File saveFolder;

    /**
     * The Q-Table. Used to store state-action pairs.
     */
    private Map<Integer, Map<Integer, Double>> qTable;

    /**
     * List to keep track of transition history.
     */
    private List<List<Number>> history = new ArrayList<>();

    /**
     * Reference to current StateSpaceManager.
     */
    private StateSpaceManager manager;

    /**
     * Change the reference to the current StateSpaceManager.
     * @param saveLocation File of StateSpaceManager.
     */
    private void setManager(File saveLocation) {
        LOGGER.fine("Setting manager");
        saveFolder = saveLocation;
        manager = StateSpaceManager.getManager(saveLocation);
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

    /**
     * Pick the max action from current state.
     * @return index for action
     */
    private int getBestAction(Map<Integer, Double> actions)
    {
        LOGGER.fine("Chose to exploit");
        return Collections.max(actions.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();
    }

    /**
     * Pick a random action from the current StateSpaceManager.
     * @return The index of the action.
     */
    private int getRandomAction()
    {
        LOGGER.fine("Chose to explore");
        Random rand = new Random();
        return rand.nextInt(manager.getNumberOfActions());
    }

    /**
     * Choose to explore at a rate `epsilon` or choose the best action.
     * @return The index of the action.
     */
    private int getActionFromEpsilonGreedy()
    {
        LOGGER.fine("Getting action from epsilon-greedy policy");
        Random rand = new Random();
        if (rand.nextDouble() < epsilon)
        {
            return getRandomAction();
        } else {
            return getBestAction(qTable.get(currentStateId));
        }
    }

    /**
     * Pick a random state from the current StateSpaceManager.
     * @return The index of the state.
     */
    private int getRandomState()
    {
        LOGGER.fine("Getting random state");
        Random rand = new Random();
        return rand.nextInt(manager.getNumberOfStates());
    }

    /**
     * Iterable method that sets the currentState, currentStep, currentEpisode.
     * - Acts as a nested for loop.
     */
    private void setCurrentIteration()
    {
        if (++currentStep == stepThreshold) {
            currentStep = 0;
            currentEpisode++;
            currentStateId = getRandomState();
        }
    }

    /**
     * Write the current qTable to qtable.csv.
     */
    private void writeQTable()
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

    /**
     * Write the current history to history.csv.
     */
    private void writeHistory()
    {
        try {
            FileWriter writer = new FileWriter(new File(saveFolder, "history.csv"));
            writer.write("State,Action,New State,Probability,Reward");
            for(List<Number> moment: history){
                String row = moment.stream().map(String::valueOf).collect(Collectors.joining(","));
                writer.write(System.lineSeparator());
                writer.write(row);
            }
            writer.close();
        } catch (IOException err)
        {
            LOGGER.severe("Unable to write history", err);
        }
    }

    private void printEstimatedTime()
    {
        int iterations = episodeThreshold * stepThreshold;
        int seconds = iterations * 47 / 1000;
        System.out.println(String.format("Iterations will take appox: %dD %dH %dM %dS",
                seconds / 86400,
                seconds % 86400 / 3600,
                seconds % 86400 % 3600 / 60,
                seconds % 86400 % 3600 % 60
                ));
    }

    /**
     * If the algorithm is still iterating and should continue iterating.
     * @return A boolean.
     */
    private boolean isIterating ()
    {
        return currentEpisode != episodeThreshold && !isForceQuitting;
    }

    /**
     * Validate a number is a number between 0 and 1.
     * @param rate the rate to validate.
     * @param name the name of the field.
     * @return boolean.
     */
    private boolean isRate (double rate, String name) {
        if (0 < rate && rate < 1)
        {
            return true;
        } else {
            LOGGER.severe(name + " must be greater than 0 or less than 1");
            return false;
        }
    }

    /**
     * If a number is greater than or equal to 0.
     * @param number the number to check.
     * @param name the name of the field.
     * @param <T> the type of number.
     * @return boolean
     */
    private <T extends Number> boolean isUnsigned (T number, String name) {
        if (number.doubleValue() > 0)
        {
            return true;
        } else {
            LOGGER.severe(name + " must be greater than or equal to 0");
            return false;
        }
    }

    /**
     * Instantiate a new QLearning Engine.
     * @param saveLocation The location where data is saved.
     */
    public QLearningEngine (File saveLocation) {
        int threshold = 1000;
        LOGGER.fine(String.format("Initialized Q-Learning engine with %d episodes and maximum of %d steps per episode", threshold, threshold));
        setManager(saveLocation);
        setNumberOfEpisodes(threshold);
        setMaxNumberOfSteps(threshold);
    }

    /**
     * Instantiate a new QLearning Engine.
     * @param saveLocation The location where data is saved.
     * @param threshold Sets both number of episodes and steps.
     * @see QLearningEngine#episodeThreshold
     * @see QLearningEngine#stepThreshold
     */
    public QLearningEngine (File saveLocation, int threshold) {
        LOGGER.fine(String.format("Initialized Q-Learning engine with %d episodes and maximum of %d steps per episode", threshold, threshold));
        setManager(saveLocation);
        setNumberOfEpisodes(threshold);
        setMaxNumberOfSteps(threshold);
    }

    /**
     * Instantiate a new QLearning Engine.
     * @param saveLocation The location where data is saved.
     * @param numOfEpisodes The number of episodes.
     * @param maxNumOfSteps The number of steps.
     * @see QLearningEngine#episodeThreshold
     * @see QLearningEngine#stepThreshold
     */
    public QLearningEngine (File saveLocation, int numOfEpisodes, int maxNumOfSteps) {
        LOGGER.fine(String.format("Initialized Q-Learning engine with %d episodes and maximum of %d steps per episode", numOfEpisodes, maxNumOfSteps));
        setManager(saveLocation);
        setNumberOfEpisodes(numOfEpisodes);
        setMaxNumberOfSteps(maxNumOfSteps);
        printEstimatedTime();
    }

    /**
     * Iterable for engine. Modifies the current state and action.
     * @return The next state-action pair.
     */
    @Override
    public int[] selectNextState() {
        if (qTable == null) {
            qTable = createQTable(manager.getNumberOfStates(), manager.getNumberOfActions());
        }
        setCurrentIteration();
        int[] step = new int[2];
        step[STATE_INDEX] = currentStateId;
        step[ACTION_INDEX] = getActionFromEpsilonGreedy();

        return step;
    }

    /**
     * Use Bellman equation to update qTable.
     * @param oldState beginning state of the excursion
     * @param action action taken
     * @param newState end state of the excursion
     * @param probability - the probability that the new state was encountered
     * @param score score of the result
     */
    @Override
    public void processResults(State oldState, List<ActionEnumeration> action, State newState, double probability, double score) {
        LOGGER.fine("Updating Q-Table value");
        int oldStateId = manager.getIDForState(oldState);
        int newStateId = manager.getIDForState(newState);
        int actionId   = manager.getIDForActions(action);

        if (qTable == null) {
            qTable = createQTable(manager.getNumberOfStates(), manager.getNumberOfActions());
        }

        double q1 = qTable.get(oldStateId).get(currentActionId);
        double q2 = qTable.get(newStateId).get(actionId);
        double v  = q1 + alpha * (score + gamma * q2 - q1);

        qTable.get(oldStateId).put(currentActionId, v);

        currentActionId = actionId;

        history.add(Arrays.asList(oldStateId, actionId, newStateId, probability, score));
    }

    /**
     * Whether the iterable should continue iterating.
     * @return boolean.
     */
    @Override
    public boolean continueIterating() {
        return isIterating();
    }

    /**
     * Finish iterating immediately and write qTable to file.
     */
    @Override
    public void finishOptimization() {
        isForceQuitting = true;
        writeQTable();
        writeHistory();
    }

    /**
     * Reset the engine to match a fresh instantiation.
     */
    @Override
    public void reset() {
        isForceQuitting = false;
        currentEpisode = 0;
        currentStep = 0;
        //qTable = createQTable(manager.getNumberOfStates(), manager.getNumberOfActions());
        currentStateId = getRandomState();
    }

    /**
     * Whether or not the engine can run in parallel.
     * @return boolean.
     */
    @Override
    public boolean requiresFixedExcursionOrdering() {
        return true;
    }

    /**
     * Get expected values of all state-action pairs.
     * @return qTable
     */
    @Override
    public Map<Integer, Map<Integer, Double>> getResults() {
        if (isIterating())
        {
            throw new QLearningEngine.PrematureInvocationException();
        }
        Map<Integer, Map<Integer, Double>> optimalPolicy = new HashMap<>();
        for(int i=0; i<qTable.size(); i++)
        {
            int bestActionId = getBestAction(qTable.get(i));
            Double bestActionValue = qTable.get(i).get(bestActionId);
            optimalPolicy.put(i, Map.ofEntries(Map.entry(bestActionId, bestActionValue)));
        }
        return optimalPolicy;
    }

    /**
     * Change the save location of the engine.
     * @param saveLocation - the location the managers are housed
     */
    @Override
    public void setSaveLocation(File saveLocation) {
        setManager(saveLocation);
    }

    /**
     * Sets the learning rate of the algorithm.
     * @param rate a value between 0 and 1.
     * @see QLearningEngine#alpha
     */
    public void setAlpha(double rate) {
        if (isRate(rate, "Learning rate")) {
            alpha = rate;
        }
    }

    /**
     * Set the learning rate of the algorithm.
     * @param rate a value between 0 and 1.
     * @see QLearningEngine#alpha
     */
    public void setLearningRate(double rate)
    {
        setAlpha(rate);
    }

    /**
     * @return the learning rate of the algorithm.
     */
    public double getAlpha ()
    {
        return alpha;
    }

    /**
     * @return the learning rate of the algorithm.
     */
    public double getLearningRate ()
    {
        return getAlpha();
    }

    /**
     * Set the exploration rate of the algorithm.
     * @param rate a value between 0 and 1.
     * @see QLearningEngine#epsilon
     */
    public void setEpsilon(double rate) {
        if (isRate(rate, "Exploration rate")) {
            epsilon = rate;
        }
    }

    /**
     * Set the exploration rate of the algorithm.
     * @param rate a value between 0 and 1.
     * @see QLearningEngine#epsilon
     */
    public void setExplorationRate(double rate)
    {
        setEpsilon(rate);
    }

    /**
     * @return the exploration rate of the algorithm.
     */
    public double getEpsilon ()
    {
        return epsilon;
    }

    /**
     * @return the exploration rate of the algorithm.
     */
    public double getExplorationRate ()
    {
        return getEpsilon();
    }

    /**
     * Sets the discount rate of the algorithm.
     * @param rate a value between 0 and 1.
     * @see QLearningEngine#gamma
     */
    public void setGamma(double rate) {
        if (isRate(rate, "Discount rate")) {
            gamma = rate;
        }
    }

    /**
     * Sets the discount rate of the algorithm.
     * @param rate a value between 0 and 1.
     * @see QLearningEngine#gamma
     */
    public void setDiscountRate(double rate)
    {
        setGamma(rate);
    }

    /**
     * @return the discount rate of the algorithm.
     */
    public double getGamma ()
    {
        return gamma;
    }

    /**
     * @return the discount rate of the algorithm.
     */
    public double getDiscountRate ()
    {
        return getGamma();
    }

    /**
     * Change the number of episodes in algorithm.
     * @param number the amount.
     * @see QLearningEngine#episodeThreshold
     */
    public void setNumberOfEpisodes(int number) {
        if (isUnsigned(number, "Number of episodes"))
        {
            episodeThreshold = number;
        }
    }

    /**
     * Change the number of episodes in algorithm.
     * @param number the amount.
     * @see QLearningEngine#stepThreshold
     */
    public void setMaxNumberOfSteps(int number) {
        if (isUnsigned(number, "Maximum number of steps"))
        {
            stepThreshold = number;
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
