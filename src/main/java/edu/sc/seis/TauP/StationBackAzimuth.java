package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;

/**
 * Calculatable ray that knows its receiver lot,lon and back azimuth of the arriving ray.
 */
public class StationBackAzimuth extends LatLonable {

    public StationBackAzimuth(LatLonLocatable staLatLon, Double backAzimuth) {
        this.staLatLon = staLatLon;
        this.backAzimuth = backAzimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        if (isGeodetic()) {
            throw new RuntimeException("geodetic not yet");
        }
        Location staLoc = staLatLon.asLocation();
        double evtLat = SphericalCoords.latFor(staLoc.getLatitude(), staLoc.getLongitude(), backAzimuth, totalDist);
        double evtLon = SphericalCoords.lonFor(staLoc.getLatitude(), staLoc.getLongitude(), backAzimuth, totalDist);
        double azimuth = SphericalCoords.azimuth(evtLat, evtLon, staLoc.getLatitude(), staLoc.getLongitude());
        out[0] = SphericalCoords.latFor(evtLat, evtLon, calcDist, azimuth);
        out[1] = SphericalCoords.lonFor(evtLat, evtLon, calcDist, azimuth);
        return out;
    }

    LatLonLocatable staLatLon;
    double backAzimuth;
}
