package edu.sc.seis.TauP;

public class LinearInterpolation {
    public static double linInterp(double xa,
                                   double xb,
                                   double ya,
                                   double yb,
                                   double x) {
        return (yb - ya) * (x - xa) / (xb - xa) + ya;
    }

    /**
     * solves the equation (yb-ya)/(xb-xa) = (y-ya)/(x-xa) for y given x. Useful
     * for finding the pixel for a value given the dimension of the area and the
     * range of values it is supposed to cover. Note, this does not check for xa ==
     * xb, in which case a divide by zero would occur.
     */
    public static double linearInterp(double xa,
                                      double ya,
                                      double xb,
                                      double yb,
                                      double x) {
        if (x == xa) {
            return ya;
        }
        if (x == xb) {
            return yb;
        }
        return (yb - ya) * (x - xa) / (xb - xa) + ya;
    }
}
