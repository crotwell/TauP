package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

/**
 * Calculatable ray that knows its source lot,lon and azimuth of departure.
 */
public class EventAzimuth extends LatLonable {
    double azimuth;

    Location evtLatLon;

    public EventAzimuth(Location evtLatLon, Double azimuth) {
        this.evtLatLon = evtLatLon;
        this.azimuth = azimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodtic not yet");
        }
        out[0] = SphericalCoords.latFor(evtLatLon.getLatitude(), evtLatLon.getLongitude(), calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLatLon.getLatitude(), evtLatLon.getLongitude(), calcDist, azimuth);
        return out;
    }
}
