package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicLine;

/**
 * Calculatable ray that knows its receiver lot,lon and back azimuth of the arriving ray.
 */
public class StationBackAzimuth extends LatLonable {

    public StationBackAzimuth(LatLonLocatable staLatLon, Double backAzimuth, Geodesic geodesic) {
        this.staLatLon = staLatLon;
        this.backAzimuth = backAzimuth;
        this.geodesic = geodesic;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        Location staLoc = staLatLon.asLocation();
        if (isGeodetic()) {
            double backDistance = totalDist - calcDist;
            GeodesicLine gLine = geodesic.ArcDirectLine(staLoc.getLatitude(), staLoc.getLongitude(), backAzimuth, backDistance);
            out[0] = gLine.Latitude();
            out[1] = gLine.Longitude();
        } else {
            // spherical
            double evtLat = SphericalCoords.latFor(staLoc.getLatitude(), staLoc.getLongitude(), backAzimuth, totalDist);
            double evtLon = SphericalCoords.lonFor(staLoc.getLatitude(), staLoc.getLongitude(), backAzimuth, totalDist);
            double azimuth = SphericalCoords.azimuth(evtLat, evtLon, staLoc.getLatitude(), staLoc.getLongitude());
            out[0] = SphericalCoords.latFor(evtLat, evtLon, calcDist, azimuth);
            out[1] = SphericalCoords.lonFor(evtLat, evtLon, calcDist, azimuth);
        }
        return out;
    }

    LatLonLocatable staLatLon;
    double backAzimuth;
}
