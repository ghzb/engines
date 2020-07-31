package optimization;

import IPC.SocketData;
import IPC.SocketServer;
import IPC.SocketWorker;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.sun.istack.logging.Logger;
import state.ActionEnumeration;
import state.State;
import state.StateEnumeration;
import state.StateSpaceManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PythonOptimizationEngine implements OptimizationEngine {
    protected transient static Logger LOGGER = Logger.getLogger(ValueIterationEngine.class);

    private StateActionCache cache;
    private ConcurrentLinkedQueue<SocketData> pythonActionQueue;
    private AtomicBoolean finished;
    private StateActionCache.VisitedStatesTracker visits;
    private QTable qTable;
    private HistoryTable history;
    private PythonRunner pythonProcess;
    private StateSpaceManager manager;

    private SocketWorker worker;
    private int curStateIndex;
    private int curActionIndex;
    private SocketServer server;
    private boolean ranThroughProcessResultsOnce = false;
    private int maxVisits;
    private File pythonPath;
    private File pythonFile;
    private File saveLocation;
    private Boolean startedThreads = false;


    /**
     * Create a new engine instance.
     * @param saveLocation - where files will be stored.
     * @param pythonFile - the path of the Python file to run.
     * @param pythonPath - the path of the Python interpreter.
     */
    public PythonOptimizationEngine(File saveLocation, File pythonFile, File pythonPath)
    {
        this.saveLocation = saveLocation;
        createInstance(pythonFile, pythonPath);
    }

    /**
     * Create a new engine instance. Try to use the default Python interpreter for executing Python files.
     * @param saveLocation - where files will be stored.
     * @param pythonFile - the path of the Python file to run.
     */
    public PythonOptimizationEngine(File saveLocation, File pythonFile)
    {
        this.saveLocation = saveLocation;
        createInstance(pythonFile, null);
    }

    /**
     * Initialize variables necessary for engine.
     * @param pythonFile - the path of the Python file to run.
     * @param pythonPath - (nullable) the path of the Python interpreter.
     */
    private void createInstance(File pythonFile, File pythonPath)
    {
        this.pythonPath = pythonPath;
        this.pythonFile = pythonFile;
        this.manager = StateSpaceManager.getManager(this.saveLocation);
        this.maxVisits = 0;
        this.qTable = new QTable(this.saveLocation);
        this.history = new HistoryTable(this.saveLocation);
        this.cache = new StateActionCache();
        this.pythonActionQueue = new ConcurrentLinkedQueue<>();
        this.finished = new AtomicBoolean(false);
        this.curStateIndex = manager.getIDForState(manager.getDefaultState());
    }

    /**
     * Start the socket server on port 8888.
     * Then spawn create and run a TAIL_runner.py file on a separate thread.
     */
    private void startSocketServerAndPythonThreads ()
    {
        initSocketServer();
        this.pythonProcess = new PythonRunner(pythonPath, pythonFile);
        pythonProcess.run();
        startedThreads = true;
    }

    /**
     * Set the maximum visits for each state before force quitting.
     * @param maxVisits - The maximum number of visits for each state.
     */
    public void setMaxVisits(int maxVisits)
    {
        this.maxVisits = maxVisits;
        this.visits = new StateActionCache.VisitedStatesTracker(manager, maxVisits);
    }

    /**
     * Determine the next state/action that should be explored
     * Reset a boolean that determines if results have already been sent to Python.
     * @return The IDs of the next state and action to explore are returned, respectively
     */
    @Override
    public int[] selectNextState() {
        if (!startedThreads)
        {
            startSocketServerAndPythonThreads();
        }
        curActionIndex = waitForNextPythonAction();

        int[] newState = new int[2];
        newState[ACTION_INDEX] = curActionIndex;
        newState[STATE_INDEX] = getRandomState();
        ranThroughProcessResultsOnce = false;
        return newState;
    }

    /**
     * Store the results of an excursion, performing any necessary housekeeping operations
     * Stores all results in a cache and only chooses one random result to send to Python.
     * This single selection allows the Java/Python IPC to stay synchronized.
     *
     * @param oldState    beginning state of the excursion
     * @param action      action taken
     * @param newState    end state of the excursion
     * @param probability - the probability that the new state was encountered
     * @param score       score of the result
     */
    @Override
    public void processResults(State oldState, List<ActionEnumeration> action, State newState, double probability, double score) {
        cache.add(curStateIndex, curActionIndex, oldState, action, newState, probability, score);
        if (!ranThroughProcessResultsOnce) {
            sendRandomCachedResultToPython();
            ranThroughProcessResultsOnce = true;
        }
//        curStateIndex = getRandomState();
    }

    /**
     * A random result is pulled from the cache.
     * The results are added to the q-table for future policy generation.
     * The data is structured as JSON for easy reading/parsing and sent off to Python.
     */
    private void sendRandomCachedResultToPython()
    {
        Result result = cache.chooseOne(curStateIndex, curActionIndex);

        System.out.println(String.format("curState: %d", curStateIndex));

        int oldStateId = manager.getIDForState(result.oldState);
        int newStateId = manager.getIDForState(result.newState);
        int actionId   = manager.getIDForActions(result.actions);

        if (maxVisits > 0)
        {
            visits.add(newStateId, actionId);
            if (visits.reachedMax())
            {
                finished.set(true);
            }
        }

        qTable.put(oldStateId, curActionIndex, result.score);
        history.add(result);

        curActionIndex = actionId;

        JsonArray obs = new JsonArray();
        for (StateEnumeration<?> state: result.oldState.getValues()){
            obs.add(((Number)state.getValueForEnum()).doubleValue());
        }
        JsonObject message = new JsonObject();
        message.add("reward", result.score);
        message.add("obs", obs);
        message.add("done", finished.get());

        worker.send("step", message.toString());
    }

    /**
     * Determine whether the optimization engine has explored enough of the state space
     *
     * @return False if the optimization engine does not need to run any more excursions
     */
    @Override
    public boolean continueIterating() {
        return !finished.get();
    }

    /**
     * Complete the optimization process
     * As a courtesy, please have this call set continueIterating() to false.
     * Otherwise, recalculateRewards inside the OptimizationManager will malfunction.
     */
    @Override
    public void finishOptimization() {
        finished.set(true);
        pythonProcess.destroy();
        server.destroy();
        System.out.println("TRYING TO FINISH OPTIMIZATION");
        qTable.writeToFile();
        history.writeToFile();
    }

    /**
     * Purge the optimization engine of any progress it has made
     */
    @Override
    public void reset() {
        LOGGER.fine("ValueIterationEngine progress reset");
        curActionIndex = 0;
        curStateIndex = 0;
        pythonProcess.destroy();
        server.destroy();
        startedThreads = false;
    }

    /**
     * States whether excursions can be run in parallel. If so, selectNextState
     * will be called again before the result of the previous run is available. This
     * should be false if the engine uses the results of a run to select the next state.
     *
     * @return whether excursions can run in parallel.
     */
    @Override
    public boolean requiresFixedExcursionOrdering() {
        return false;
    }

    /**
     * Retrieve the optimal policy.
     * From the Q-table.
     *
     * @return A mapping from every state to its optimal action and expected utility is returned
     */
    @Override
    public Map<Integer, Map<Integer, Double>> getResults() {
        if (!finished.get())
        {
            throw new Exceptions.PrematureInvocationException();
        }
        return qTable.getOptimalPolicy();
    }

    /**
     * Informs the engine where the managers are housed, in case it needs to access the state space manager
     *
     * @param saveLocation - the location the managers are housed
     */
    @Override
    public void setSaveLocation(File saveLocation) {
        this.saveLocation = saveLocation;
    }

    /**
     * @return the index of a random state.
     */
    private int getRandomState()
    {
        Random rand = new Random();
        return rand.nextInt(manager.getNumberOfStates());
    }

    /**
     * Establish a new socket server and add event listeners for channels.
     */
    private void initSocketServer()
    {
        server = new SocketServer();
//        server.on("$MESSAGE", this::onSocketChannel__$MESSAGE);
        server.on("$ISSUE", this::onSocketChannel__$ISSUE);
        server.on("$CONNECT", this::onSocketChannel__$CONNECT);
        server.on("$DISCONNECT", this::onSocketChannel__$DISCONNECT);
        server.on("debug", this::onSocketChannel__debug);
        server.on("step", this::onSocketChannel__step);
        server.on("reset", this::onSocketChannel__reset);
    }

    /**
     * This method runs every time python sends data. No matter which channel. This is typically used for debugging
     * purposes.
     * @param socket data received from the socket
     */
    private void onSocketChannel__$MESSAGE(SocketData socket)
    {
        System.out.println(socket);
    }

    /**
     * Fatal error trace-backs in Python are sent to this channel.
     * @param socket data received from the socket
     */
    private void onSocketChannel__$ISSUE(SocketData socket)
    {
        try {
            socket.error.printStackTrace();
        } catch (NullPointerException err)
        {
            LOGGER.severe(socket.message);
        } catch (Exception err)
        {
            err.printStackTrace();
        }
    }


    /**
     * Gym environments require a "shape" to create a observation/action space. For example (3,3,3) would be the
     * equivalent of `int[3][3][3]` in Java. However, some python environments use discrete spaces which do not
     * have a shape. This requires Java to send the total number of states/actions.
     * @param socket data received from the socket
     */
    private void onSocketChannel__$CONNECT(SocketData socket)
    {
        worker = socket.worker;
        JsonObject json = Json.object();
        json.add("num_of_states", manager.getNumberOfStates());
        json.add("num_of_actions", manager.getNumberOfActions());
        json.add("shape_of_states", manager.getPossibleStates().size());
        json.add("state_bounds", convertToJsonArray(getStateBounds()));

        worker.send("state_space", json.toString());
    }


    /**
     * When the Python process disconnects from the socket. This may be caused from unexpected or successful termination.
     * @param socket data received from the socket
     */
    private void onSocketChannel__$DISCONNECT(SocketData socket)
    {
        finished.set(true);
        System.out.println("disconnected");
    }

    /**
     * Reprint debug messages from Python which may contain different colors.
     * @param socket data received from the socket
     */
    private void onSocketChannel__debug(SocketData socket)
    {
//        System.out.println("\t\t" + socket.message);
    }

    /**
     * When the gym calls the step method, relevant data is sent in JSON format so Java can update its internal records.
     * @param socket data received from the socket
     */
    private void onSocketChannel__step(SocketData socket)
    {
        pythonActionQueue.add(socket);
    }

    /**
     * This message is sent when Python wants to reset the gym environment.
     * Java will send the default state and update its internal record of the environment.
     * @param socket data received from the socket
     */
    private void onSocketChannel__reset(SocketData socket)
    {
        State state = manager.getDefaultState();
        JsonArray resetValues = new JsonArray();
        for (StateEnumeration<?> enumeration : state.getValues())
        {
            resetValues.add(((Number)enumeration.getDefaultValue().getValueForEnum()).doubleValue());
        }
        System.out.println("ACK reset");
        socket.worker.send("reset", resetValues.toString());
    }

    /**
     * Convert a list of any type to JSON.
     * @param list - A list of anything.
     * @return JsonArray
     */
    private JsonArray convertToJsonArray(List<?> list)
    {
        JsonArray json = new JsonArray();
        for (var item : list)
        {
            if (item instanceof List)
            {
                json.add(convertToJsonArray((List<?>)item));
            } else if (item instanceof Number) {
                json.add(((Number) item).doubleValue());
            } else {
                json.add(String.valueOf(item));
            }
        }
        return json;
    }

    /**
     *
     * @return The min and max values for each registered StateEnumeration
     */
    private List<List<Number>> getStateBounds()
    {
        List<List<Number>> bounds = new ArrayList<>();
        for (StateEnumeration<?> state : manager.getPossibleStates())
        {
            List<Number> values = new ArrayList<>();
            for (StateEnumeration<?> thing : state.getClass().getEnumConstants())
            {
                values.add((Number) thing.getValueForEnum());
            }
            values.sort((a, b) -> {
                double c = a.doubleValue() - b.doubleValue();
                return c > 0 ? 1 : c < 0 ? -1 : 0;
            });
            Number min = values.get(0);
            Collections.reverse(values);
            Number max = values.get(0);
            bounds.add(Arrays.asList(min, max));
        }
        return bounds;
    }


    /**
     * Since the socket events execute on another thread, this method is called to pull the event data to the main
     * thread. Without this blocking method, selectNextState would continue with or without python changes.
     */
    private int waitForNextPythonAction()
    {
        SocketData socket;
        do {
            socket = pythonActionQueue.poll();
        } while (socket == null);
        return Integer.parseInt(socket.message);
    }

}
