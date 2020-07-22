package test.optimization;

import optimization.Correlation;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Zeb Burroughs
 */
public class CorrelationTest {
    @Test
    public void Correlation ()
    {
        double[] a1 = {1};
        double[] b1 = {1};

        assertTrue(Double.isNaN(Correlation.fromArray(a1, b1)));

        double[] a2 = {1,5};
        double[] b2 = {-1,2};

        assertEquals(Correlation.fromArray(a2, b2), 1.0, 0);

        double[] a3 = {1,2,3,4,5};
        double[] b3 = {1,-2,-3,4,-5};

        assertNotEquals(Math.abs(Correlation.fromArray(a3, b3)), 1);
    }
}
