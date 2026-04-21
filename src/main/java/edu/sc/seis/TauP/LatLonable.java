package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

/**
 * Provided by calculatable rays that have knowledge of lat and lon for the path.
 */
public abstract class LatLonable {

    public LatLonable(DistanceCalc distCalc) {
        this.distCalc = distCalc;
    }

    public abstract double[] calcLatLon(double calcDist, double totalDist, double depthKm);

    public DistanceCalc getDistCalc() {
        return distCalc;
    }

    DistanceCalc distCalc;
}
