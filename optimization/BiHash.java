package optimization;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Zeb Burroughs
 *
 * A quick extension of HashMap that can be inverted.
 *
 * @param <K> The key
 * @param <V> The Value
 */
public class BiHash<K,V> extends HashMap<K, V> {
    /**
     * Invert the current map. Keys become values and vice versa.
     * @return Inverted map.
     */
    public BiHash<V,K> inverse()
    {
        BiHash<V,K> inverted = new BiHash<>();
        for(Map.Entry<K, V> entry : this.entrySet())
        {
            inverted.put(entry.getValue(), entry.getKey());
        }
        return inverted;
    }
}
