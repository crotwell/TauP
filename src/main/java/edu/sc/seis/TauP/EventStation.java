package edu.sc.seis.TauP;


import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;

/**
 * Calculatable ray from a source lat,lon to a receiver lat,lon.
 */
public class EventStation extends LatLonable {

    public EventStation(LatLonLocatable evt, LatLonLocatable sta) {
        this.evt = evt;
        this.sta = sta;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodetic not yet");
        }
        Location evtLoc = evt.asLocation();
        Location staLoc = sta.asLocation();
        double azimuth = SphericalCoords.azimuth(evtLoc, staLoc);
        out[0] = SphericalCoords.latFor(evtLoc, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLoc, calcDist, azimuth);
        return out;
    }

    LatLonLocatable evt;
    LatLonLocatable sta;
}
