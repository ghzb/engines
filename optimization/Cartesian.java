package optimization;

import java.util.*;
import java.util.stream.Collectors;

public class Cartesian {


    public static <T> List<List<T>> productFrom(List<List<T>> list)
    {
        List<List<T>> result = new ArrayList<>();
        for(T item: list.get(0))
        {
            result.add(Arrays.asList(item));
        }

        for (int i = 1; i < list.size(); i++)
        {
            List<T> S = list.get(i);
            List<List<T>> newResult = new ArrayList<>();
            for(List<T> L: result)
            {
                for(T item: S)
                {
                    List<T> lc = new ArrayList<>(L);
                    lc.add(item);
                    newResult.add(lc);
                }
            }
            result = newResult;
        }

        return result;
    }


}
