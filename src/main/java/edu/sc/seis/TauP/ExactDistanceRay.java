package edu.sc.seis.TauP;

import java.util.ArrayList;
import java.util.List;

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

}
