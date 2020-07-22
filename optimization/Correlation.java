package optimization;


/**
 *
 * @author Zeb Burroughs
 */
public class Correlation {
    public static double fromArray(double[] a, double[] b)
    {

        if (a == null || b == null || a.length != b.length) {
            throw new Correlation.UnequalArrayLengths();
        }

        int n = a.length;
        double sx, sy, sxx, sxy, syy;
        sx = sy = sxx = sxy = syy = 0;

        for (int i = 0; i < n; i++)
        {
            double x = a[i];
            double y = b[i];

            sx += x;
            sy += y;
            sxx += x * x;
            sxy += x * y;
            syy += y * y;
        }

        double cov = sxy / n - sx * sy / n / n;
        double errx = Math.sqrt(sxx / n - sx * sx / n / n);
        double erry = Math.sqrt(syy / n - sy * sy / n / n);

        return cov / errx / erry;
    }

    public static class UnequalArrayLengths extends NullPointerException
    {
        @Override
        public String getMessage()
        {
            return "Arrays must not be null and must be the same length.";
        }
    }
}
