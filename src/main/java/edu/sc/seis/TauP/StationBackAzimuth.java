package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.Location;

/**
 * Calculatable ray that knows its receiver lot,lon and back azimuth of the arriving ray.
 */
public class StationBackAzimuth extends LatLonable {

    public StationBackAzimuth(Location staLatLon, Double backAzimuth) {
        this.staLatLon = staLatLon;
        this.backAzimuth = backAzimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodetic not yet");
        }
        double evtLat = SphericalCoords.latFor(staLatLon.getLatitude(), staLatLon.getLongitude(), backAzimuth, totalDist);
        double evtLon = SphericalCoords.lonFor(staLatLon.getLatitude(), staLatLon.getLongitude(), backAzimuth, totalDist);
        double azimuth = SphericalCoords.azimuth(evtLat, evtLon, staLatLon.getLatitude(), staLatLon.getLongitude());
        out[0] = SphericalCoords.latFor(evtLat, evtLon, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLat, evtLon, calcDist, azimuth);
        return out;
    }

    Location staLatLon;
    double backAzimuth;
}
