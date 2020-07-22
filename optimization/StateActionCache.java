package optimization;

import state.ActionEnumeration;
import state.State;

import java.util.*;

/**
 * The cache stores a list of all results based on the state/action pair.
 * selectNext state will perform a lookup. If it exists, it will use the cached data.
 * If the lookup is unsuccessful, the cache will be populated during processResults.
 */
public class StateActionCache {

    public static class Result {
        public State oldState;
        public List<ActionEnumeration> actions;
        public State newState;
        public double probability;
        public double score;
        public Result(State oldState, List<ActionEnumeration> actions, State newState, double probability, double score){
            this.oldState = oldState;
            this.actions = actions;
            this.newState = newState;
            this.probability = probability;
            this.score = score;
        }
    }

    private final Map<int[], List<Result>> cache = new HashMap<>();
    private final List<Result> defaultResult = new ArrayList<>();

    private List<Result> get(int stateIndex, int actionIndex)
    {
        int[] key = new int[]{stateIndex, actionIndex};
        return cache.getOrDefault(key, defaultResult);
    }

    private void put (int stateIndex, int actionIndex, List<Result> results)
    {
        int[] key = new int[]{stateIndex, actionIndex};
        cache.put(key, results);
    }

    public Boolean has(int stateIndex, int actionIndex)
    {
        return cache.containsKey(new int[]{stateIndex, actionIndex});
    }

    public void add(int stateIndex, int actionIndex, State oldState, List<ActionEnumeration> action, State newState, double probability, double score)
    {
        List<Result> results = get(stateIndex, actionIndex);
        results.add(new Result(oldState, action, newState, probability, score));
        put(stateIndex, actionIndex, results);
    }

    public Result chooseOne(int stateIndex, int actionIndex)
    {
        return get(stateIndex, actionIndex).get(0);
    }
}
