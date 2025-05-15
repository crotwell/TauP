package edu.sc.seis.TauP;

/**
 * Provided by calculatable rays that have knowledge of lat and lon for the path.
 */
public abstract class LatLonable {

    public abstract double[] calcLatLon(double calcDist, double totalDist);

    public boolean isGeodetic() {
        return this.geodetic;
    }

    boolean geodetic = false;

}
