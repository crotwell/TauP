package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

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

            double km = backDistance*DtoR*DistanceRay.averageRadiusKm(geodesic);
            GeodesicLine gLine = new GeodesicLine(geodesic, staLoc.getLatitude(), staLoc.getLongitude(), backAzimuth);
            GeodesicData gdata = gLine.Position(km*1000);
            out[0] = gdata.lat2;
            out[1] = gdata.lon2;
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
