package edu.sc.seis.TauP;


import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

public class FibonacciSphere {
    public static List<SphericalCoordinate> calc(int num_pts) {
        List<SphericalCoordinate> result = new ArrayList<>(num_pts);
        for (int i = 0; i < num_pts; i++) {
            double index = 0.5+i;
            double phi = Math.acos(1 - 2 * index / num_pts);
            double theta = (Math.PI * (1 + SQRT_FIVE) * index) % TWO_PI;
            result.add(new SphericalCoordinate(phi, theta));
        }
        return result;
    }

    public static final double TWO_PI = Math.PI*2;
    public static final double SQRT_FIVE = Math.pow(5, 0.5);
}
