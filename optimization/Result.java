package optimization;

import state.ActionEnumeration;
import state.State;

import java.util.List;

/**
 * A class used to model the results return from processResults.
 * Used to quickly store and serialized results in tables.
 */
public class Result {
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

    @Override
    public String toString() {
        return "Result{" +
                "probability=" + probability +
                '}';
    }
}
