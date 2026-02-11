package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

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
            GeodesicData gdata = DistAzKarney.calcLocationDeg(evtLoc, azimuth, calcDist, geodesic);
            out[0] = gdata.lat2;
            out[1] = gdata.lon2;
        } else {
            // spherical
            out[0] = SphericalCoords.latFor(evtLoc.getLatitude(), evtLoc.getLongitude(), calcDist, azimuth);
            out[1] = SphericalCoords.lonFor(evtLoc.getLatitude(), evtLoc.getLongitude(), calcDist, azimuth);
        }
        return out;
    }
}
