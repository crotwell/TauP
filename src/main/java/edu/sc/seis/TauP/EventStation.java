package edu.sc.seis.TauP;


import edu.sc.seis.seisFile.Location;

/**
 * Calculatable ray from a source lat,lon to a receiver lat,lon.
 */
public class EventStation extends LatLonable {

    public EventStation(Location evt, Location sta) {
        this.evt = evt;
        this.sta = sta;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodtic not yet");
        }
        double azimuth = SphericalCoords.azimuth(evt, sta);
        out[0] = SphericalCoords.latFor(evt, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evt, calcDist, azimuth);
        return out;
    }

    Location evt;
    Location sta;
}
