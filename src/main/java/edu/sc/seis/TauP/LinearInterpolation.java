package edu.sc.seis.TauP;

public class LinearInterpolation {
    public static double linInterp(double xa,
                                   double xb,
                                   double ya,
                                   double yb,
                                   double x) {
        return (yb - ya) * (x - xa) / (xb - xa) + ya;
    }
}
