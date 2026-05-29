package edu.sc.seis.TauP;


import edu.sc.seis.seisFile.Location;

import java.util.ArrayList;
import java.util.List;

public class FibonacciSphere {

    public static List<SphericalCoordinate> calc(int num_pts) {
        List<SphericalCoordinate> result = new ArrayList<>(num_pts);
        for (int i = 0; i < num_pts; i++) {
            result.add(createForIndex(i, num_pts));
        }
        return result;
    }

    public static List<SphericalCoordinate> calcHemisphere(int num_pts, boolean lowerHemisphere) {
        List<SphericalCoordinate> result = new ArrayList<>();
        int doubleNumPoints = 2*num_pts;
        for (int i = 0; i < doubleNumPoints; i++) {
            SphericalCoordinate coord = createForIndex(i, doubleNumPoints);
            if (lowerHemisphere) {
                if (coord.getTakeoffAngleDegree()<=90) {
                    result.add(coord);
                }
            } else if (coord.getTakeoffAngleDegree()>=90) {
                result.add(coord);
            }
        }
        return result;
    }

    public static SphericalCoordinate createForIndex(int i, int num_pts) {
        double index = 0.5+i;
        double phi = Math.acos(1 - 2 * index / num_pts);
        double theta = (Math.PI * (1 + SQRT_FIVE) * index) % TWO_PI;
        return new SphericalCoordinate(phi, theta);
    }

    public static final double TWO_PI = Math.PI*2;
    public static final double SQRT_FIVE = Math.pow(5, 0.5);
}
