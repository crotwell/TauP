package edu.sc.seis.TauP.CLI;

import edu.sc.seis.TauP.DistanceRay;

public class Scatterer {
    public Scatterer(double depth, double dist) {
        this.depth = depth;
        double degrees = dist;
        if (degrees > 180.0 || degrees <= -180.0) {
            degrees = (180.0 + degrees) % 360.0 - 180.0;
        }
        if (degrees == -180.0) {
            degrees = 180;
        }
        this.dist = DistanceRay.ofDegrees(degrees);
    }
    public final double depth;
    public final DistanceRay dist;
}
