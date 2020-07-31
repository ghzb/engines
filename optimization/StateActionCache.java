package optimization;

import state.ActionEnumeration;
import state.State;
import state.StateSpaceManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The cache stores a list of all results based on the state/action pair.
 * selectNext state will perform a lookup. If it exists, it will use the cached data.
 * If the lookup is unsuccessful, the cache will be populated during processResults.
 */
public class StateActionCache {

    private static class StateActionPair
    {
        int stateIndex;
        int actionIndex;
        public StateActionPair (int stateIndex, int actionIndex)
        {
            this.stateIndex = stateIndex;
            this.actionIndex = actionIndex;
        }

        @Override
        public String toString() {
            return "StateActionPair{" +
                    "stateIndex=" + stateIndex +
                    ", actionIndex=" + actionIndex +
                    '}';
        }
    }

    public static class VisitedStatesTracker
    {
        private final StateSpaceManager manager;
        private final int maxVisits;

        private Map<String, Integer> visitedStates;

        @Override
        public String toString() {
            return String.format("%d/%d", Collections.max(visitedStates.values()), maxVisits);
        }

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
         */
        public void add(int stateIndex, int actionIndex)
        {
            StateActionPair key = new StateActionPair(stateIndex, actionIndex);
            visitedStates.put(key.toString(), visitedStates.getOrDefault(key.toString(), 0) + 1);
        }
    }

    private final Map<String, List<Result>> cache = new HashMap<>();

    private List<Result> get(int stateIndex, int actionIndex)
    {
        StateActionPair key = new StateActionPair(stateIndex, actionIndex);
        List<Result> results = cache.get(key.toString());
        if (results == null)
        {
            results = new ArrayList<>();
        }
        return results;
    }

    private void put (int stateIndex, int actionIndex, List<Result> results)
    {
        StateActionPair key = new StateActionPair(stateIndex, actionIndex);
        cache.put(key.toString(), results);
    }

    public Boolean has(int stateIndex, int actionIndex)
    {
        StateActionPair key = new StateActionPair(stateIndex, actionIndex);
        return cache.containsKey(key.toString());
    }

    public void add(int stateIndex, int actionIndex, State oldState, List<ActionEnumeration> action, State newState, double probability, double score)
    {
//        System.out.println(String.format("Adding to cache: %d, %d", stateIndex, actionIndex));
        List<Result> results = get(stateIndex, actionIndex);
        results.add(new Result(oldState, action, newState, probability, score));
        put(stateIndex, actionIndex, results);
    }

    public int size()
    {
        return cache.size();
    }

    public Result chooseOne(int stateIndex, int actionIndex)
    {
        WeightedRandom<Double> random = new WeightedRandom<>();
        List<Result> results = get(stateIndex, actionIndex);
        List<Double> probs = results.stream().map((Result r) -> r.probability).collect(Collectors.toList());
        random.addAll(probs);
        int index = random.nextIndex();
//        System.out.println(this);
//        System.out.println(String.format("size: %d, %d, %d: %s, %d", results.size(), stateIndex, actionIndex, probs, index));
        return results.get(index);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("StateActionCache[").append(cache.size()).append("]{");
        for (List<Result> results : cache.values())
        {
            out.append("results[").append(results.size()).append("]{").append(results).append("}");
        }
        out.append("}");

        return out.toString();
    }
}
