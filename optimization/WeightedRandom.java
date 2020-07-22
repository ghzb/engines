package optimization;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Zeb Burroughs
 */
public class WeightedRandom<T extends Number> {
    private final Map<Integer, T> distribution;
    private Double sum;
    private List<?> _list;

    public WeightedRandom(){
        distribution = new HashMap<>();
        sum = 0d;
    }

    public void add(T probability) {
        this.distribution.put(distribution.size(), (T) probability);
        sum += probability.doubleValue();
    }

    public void addAll(List<T> probabilities)
    {
        probabilities.forEach(this::add);
    }

    public void mapToList(List<?> list)
    {
        _list = list;
    }

    public int nextIndex () {

        double ratio = 1.0d / sum;
        double dist = 0;
        double rand = Math.random();
        for (Integer i: distribution.keySet())
        {
            dist += distribution.get(i).doubleValue();
            if (rand / ratio <= dist)
            {
                return i;
            }
        }
        return 0;
    }

    public Object nextItem ()
    {
        return _list.get(nextIndex());
    }
}
