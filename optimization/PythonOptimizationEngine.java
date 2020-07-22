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
    protected transient static Logger LOGGER = Logger.getLogger(PythonOptimizationEngineOld.class);

    private final StateActionCache cache;
    private final ConcurrentLinkedQueue<SocketData> pythonActionQueue;
    private final AtomicBoolean finished;
    private final Helpers.VisitedStatesTracker visits;
    private final Helpers.QTable qTable;
    private final Helpers.HistoryTable history;
    private final PythonRunner pythonProcess;
    private final StateSpaceManager manager;

    private File saveFolder;
    private int curStateIndex;
    private int curActionIndex;
    private SocketServer server;



    public PythonOptimizationEngine(File saveFolder, File pythonPath, File pythonFile)
    {
        this.saveFolder = saveFolder;
        this.manager = StateSpaceManager.getManager(saveFolder);
        this.visits = new Helpers.VisitedStatesTracker(manager, manager.getNumberOfActions());
        this.qTable = new Helpers.QTable(manager, saveFolder);
        this.history = new Helpers.HistoryTable(saveFolder);
        this.cache = new StateActionCache();
        this.pythonActionQueue = new ConcurrentLinkedQueue<SocketData>();
        this.finished = new AtomicBoolean(false);

        initSocketServer();
        this.pythonProcess = new PythonRunner(pythonPath, pythonFile);
        pythonProcess.run();
    }

    /**
     * Determine the next state/action that should be explored
     *
     * @return The IDs of the next state and action to explore are returned, respectively
     */
    @Override
    public int[] selectNextState() {
        System.out.println("waiting for next state");
        curActionIndex = waitForNextPythonAction();
        System.out.println("received next state");
        if (cache.has(curStateIndex, curActionIndex))
        {
            sendRandomCachedResultToPython();
        }
        int[] newState = new int[2];
        newState[ACTION_INDEX] = curActionIndex;
        newState[STATE_INDEX] = curStateIndex;
        return newState;
    }

    /**
     * Store the results of an excursion, performing any necessary housekeeping operations
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
        visits.reset();
        pythonProcess.destroy();
        server.destroy();
        initSocketServer();
        pythonProcess.run();
        server.waitForConnection();
    }

    /**
     * States whether excursions can be run in parallel. If so, selectNextState
     * will be called again before the result of the previous run is available. This
     * should be false if the engine uses the results of a run to select the next state.
     *
     * @return
     */
    @Override
    public boolean requiresFixedExcursionOrdering() {
        return false;
    }

    /**
     * Retrieve the optimal policy
     *
     * @return A mapping from every state to its optimal action and expected utility is returned
     */
    @Override
    public Map<Integer, Map<Integer, Double>> getResults() {
        if (!finished.get())
        {
            throw new Helpers.PrematureInvocationException();
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
        saveFolder = saveLocation;
    }

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
     * Fatal error tracebacks in Python are sent to this channel.
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
        JsonObject json = Json.object();
        json.add("num_of_states", manager.getNumberOfStates());
        json.add("num_of_actions", manager.getNumberOfActions());
        json.add("shape_of_states", manager.getPossibleStates().size());
        json.add("shape_of_actions", getShapeOfActions());
        json.add("state_bounds", convertToJsonArray(getStateBounds()));

        socket.worker.send("state_space", json.toString());
        System.out.println(String.format("Sending connection data: %s", json));
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
        System.out.println("\t\t" + socket.message);
    }

    /**
     * When the gym calls the step method, relevant data is sent in JSON format so Java can update its internal records.
     * @param socket data received from the socket
     */
    private void onSocketChannel__step(SocketData socket)
    {
        System.out.println(String.format("received action: %s", socket.message));
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
     * Generates a "shape" from actions. Each registered action will add to the shape.
     * 2 registered actions may return (3,3) == int[3][3]
     * 3 registered actions may return (3,4,3) == int[3][4][3]
     * @return JsonArray for sending accross socket
     */
    private JsonArray getShapeOfActions()
    {
        JsonArray shape_of_actions = new JsonArray();
        for(ActionEnumeration action : manager.getPossibleActions())
        {
            int size = action.getClass().getEnumConstants().length;
            shape_of_actions.add(size);
        }
        return shape_of_actions;
    }

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

    private void sendRandomCachedResultToPython()
    {
        StateActionCache.Result result = cache.chooseOne(curStateIndex, curActionIndex);

        visits.add(result.newState);
        if (visits.reachedMax())
        {
            finished.set(true);
        }

        int oldStateId = manager.getIDForState(result.oldState);
        int newStateId = manager.getIDForState(result.newState);
        int actionId   = manager.getIDForActions(result.actions);
        qTable.put(oldStateId, curActionIndex, result.score);
        curActionIndex = actionId;
        history.add(result);

        JsonArray obs = new JsonArray();
        for (StateEnumeration<?> state: result.newState.getValues()){
            obs.add(((Number)state.getValueForEnum()).doubleValue());
        }
        JsonObject message = new JsonObject();
        message.add("reward", result.score);
        message.add("obs", obs);
        message.add("done", finished.get());
    }


}
