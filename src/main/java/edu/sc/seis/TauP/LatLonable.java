package edu.sc.seis.TauP;

public abstract class LatLonable {

    public abstract double[] calcLatLon(double calcDist, double totalDist);

    public boolean isGeodetic() {
        return this.geodetic;
    }

    boolean geodetic = false;

}
