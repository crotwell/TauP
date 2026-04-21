package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import java.util.Arrays;
import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

/**
 * Calculatable ray that knows its receiver lot,lon and back azimuth of the arriving ray.
 */
public class StationBackAzimuth extends LatLonable {

    public StationBackAzimuth(LatLonLocatable staLatLon, Double backAzimuth, DistanceCalc distCalc) {
        super(distCalc);
        this.staLatLon = staLatLon;
        this.backAzimuth = backAzimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist, double depthKm) {
        Location staLoc = staLatLon.asLocation();
        double backDistance = totalDist - calcDist;

        LatLonSimple evt = distCalc.locForAzimuthDeg(staLoc, backAzimuth, backDistance, depthKm);
        Location evtLoc = evt.asLocation();
        return new double[] {evtLoc.getLatitude(), evtLoc.getLongitude(), evtLoc.getDepthKm()};

    }

    LatLonLocatable staLatLon;
    double backAzimuth;
}
