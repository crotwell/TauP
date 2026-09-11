package edu.sc.seis.TauP;

import net.sf.geographiclib.Geodesic;

/**
 * Provided by calculatable rays that have knowledge of lat and lon for the path.
 */
public abstract class LatLonable {

    public LatLonable(DistanceCalc distCalc) {
        this.distCalc = distCalc;
    }

    /**
     * Calculates lat,lon for a distance along a path from the source.
     * @param calcDist distance to calculate lat, lon for
     * @param totalDist total distance of the path
     * @param depthKm source depth in km below the surface
     * @return lat lon of the point
     */
    public abstract double[] calcLatLon(double calcDist, double totalDist, double depthKm);

    public DistanceCalc getDistCalc() {
        return distCalc;
    }

    DistanceCalc distCalc;
}
