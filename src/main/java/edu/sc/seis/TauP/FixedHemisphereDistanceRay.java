package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.TWOPI;

/**
 * Allows equivalent distances modulo 360, but not 180-d, so that -5 is not same as 5, but is same as 355.
 */
public class FixedHemisphereDistanceRay extends DistanceRay {

    public FixedHemisphereDistanceRay() {

    }

    public static FixedHemisphereDistanceRay ofDegrees(double deg) {
        FixedHemisphereDistanceRay val = new FixedHemisphereDistanceRay();
        val.degrees = deg;
        return val;
    }

    public static FixedHemisphereDistanceRay ofKilometers(double km) {
        FixedHemisphereDistanceRay val = new FixedHemisphereDistanceRay();
        val.kilometers = km;
        return val;
    }
    public static FixedHemisphereDistanceRay ofRadians(double rad) {
        FixedHemisphereDistanceRay val = new FixedHemisphereDistanceRay();
        val.radians = rad;
        return val;
    }

    public boolean isNegativeHemisphere() {
        double R = 6371; // doesn't matter here as just want sign
        return getRadians(R) % (2*Math.PI) > Math.PI;
    }

    @Override
    public List<Arrival> calcSimplePhase(SimpleSeismicPhase phase) {
        return fixNegDistance(super.calcSimplePhase(phase));
    }

    @Override
    public List<Arrival> calcScatteredPhase(ScatteredSeismicPhase phase) {
        return fixNegDistance(super.calcScatteredPhase(phase));
    }

    public List<Arrival> fixNegDistance(List<Arrival> arrivalList) {
        List<Arrival> out = new ArrayList<>();
        for (Arrival arrival : arrivalList ) {
            if (isNegativeHemisphere() && arrival.getDist() > 0) {
                out.add(arrival.negateDistance());
            } else {
                out.add(arrival);
            }
        }
        return out;
    }

    public List<Double> calcRadiansInRange(double minRadian, double maxRadian, double radius, boolean phaseBothHemisphere) {
        List<Double> out = new ArrayList<>();
        double radianVal = getRadians(radius) % (TWOPI); // 0 <= r < 2 Pi
        if (phaseBothHemisphere) {
            if (radianVal > Math.PI) {
                radianVal = TWOPI - radianVal;
            } else if (radianVal < 0) {
                radianVal *= -1;
            }
        }
        if ((radianVal-minRadian) % TWOPI == 0.0) {
            out.add(minRadian);
        }
        int n = (int) Math.floor(minRadian/TWOPI);
        while(n * TWOPI  < maxRadian) {
            double searchVal = n * TWOPI + radianVal;
            if (minRadian < searchVal && searchVal <= maxRadian) {
                out.add(searchVal);
            }
            n++;
        }
        return out;
    }


    public String toString() {
        return"exact hemi "+super.toString();
    }
}
