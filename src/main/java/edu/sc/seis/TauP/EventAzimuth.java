package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;

/**
 * Calculatable ray that knows its source lot,lon and azimuth of departure.
 */
public class EventAzimuth extends LatLonable {
    double azimuth;

    LatLonLocatable evtLatLon;

    public EventAzimuth(LatLonLocatable evtLatLon, Double azimuth) {
        this.evtLatLon = evtLatLon;
        this.azimuth = azimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodetic not yet");
        }
        Location evtLoc = evtLatLon.asLocation();
        out[0] = SphericalCoords.latFor(evtLoc.getLatitude(), evtLoc.getLongitude(), calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLoc.getLatitude(), evtLoc.getLongitude(), calcDist, azimuth);
        return out;
    }
}
