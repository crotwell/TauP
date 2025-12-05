package edu.sc.seis.TauP;


import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicLine;

/**
 * Calculatable ray from a source lat,lon to a receiver lat,lon.
 */
public class EventStation extends LatLonable {

    public EventStation(LatLonLocatable evt, LatLonLocatable sta, Geodesic geodesic) {
        this.evt = evt;
        this.sta = sta;
        this.geodesic = geodesic;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist) {
        double[] out =  new double[2];
        Location evtLoc = evt.asLocation();
        Location staLoc = sta.asLocation();
        if (isGeodetic()) {
            GeodesicLine gLine = geodesic.InverseLine(evtLoc.getLatitude(), evtLoc.getLongitude(),
                    staLoc.getLatitude(), staLoc.getLongitude());
            out[0] = gLine.Latitude();
            out[1] = gLine.Longitude();
        } else {
            double azimuth = SphericalCoords.azimuth(evtLoc, staLoc);
            out[0] = SphericalCoords.latFor(evtLoc, calcDist, azimuth);
            out[1] = SphericalCoords.lonFor(evtLoc, calcDist, azimuth);
        }
        return out;
    }

    LatLonLocatable evt;
    LatLonLocatable sta;
}
