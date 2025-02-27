package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculatable ray that covers an exact distance, disallowing n*360-x and n*360+x.
 */
public class ExactDistanceRay extends DistanceRay {

    ExactDistanceRay() {

    }

    public static ExactDistanceRay ofDegrees(double deg) {
        ExactDistanceRay val = new ExactDistanceRay();
        val.degrees = deg;
        return val;
    }

    public static ExactDistanceRay ofKilometers(double km) {
        ExactDistanceRay val = new ExactDistanceRay();
        val.kilometers = km;
        return val;
    }

    public static ExactDistanceRay ofRadians(double rad) {
        ExactDistanceRay val = new ExactDistanceRay();
        val.radians = rad;
        return val;
    }

    public List<Double> calcRadiansInRange(double minRadian, double maxRadian, double radius, boolean phaseBothHemisphere) {
        double radianVal = getRadians(radius);
        if (phaseBothHemisphere) {
            radianVal = Math.abs(radianVal);
        }
        List<Double> out = new ArrayList<>();
        if (minRadian <= radianVal && radianVal <= maxRadian) {
            out.add(radianVal);
        }
        return out;
    }

    public String toString() {
        return"exactly "+super.toString();
    }
}
