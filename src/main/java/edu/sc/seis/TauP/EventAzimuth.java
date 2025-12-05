package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicLine;

/**
 * Calculatable ray that knows its source lot,lon and azimuth of departure.
 */
public class EventAzimuth extends LatLonable {
    double azimuth;

    LatLonLocatable evtLatLon;

    public EventAzimuth(LatLonLocatable evtLatLon, Double azimuth, Geodesic geodesic) {
        this.evtLatLon = evtLatLon;
        this.azimuth = azimuth;
        this.geodesic = geodesic;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        Location evtLoc = evtLatLon.asLocation();
        if (isGeodetic()) {
            GeodesicLine gLine = geodesic.ArcDirectLine(evtLoc.getLatitude(), evtLoc.getLongitude(), azimuth, calcDist);
            out[0] = gLine.Latitude();
            out[1] = gLine.Longitude();
        } else {
            // spherical
            out[0] = SphericalCoords.latFor(evtLoc.getLatitude(), evtLoc.getLongitude(), calcDist, azimuth);
            out[1] = SphericalCoords.lonFor(evtLoc.getLatitude(), evtLoc.getLongitude(), calcDist, azimuth);
        }
        return out;
    }
}
