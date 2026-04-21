package edu.sc.seis.TauP;

import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

/**
 * Calculatable ray that knows its source lot,lon and azimuth of departure.
 */
public class EventAzimuth extends LatLonable {
    double azimuth;

    LatLonLocatable evtLatLon;

    public EventAzimuth(LatLonLocatable evtLatLon, Double azimuth, DistanceCalc distCalc) {
        super(distCalc);
        this.evtLatLon = evtLatLon;
        this.azimuth = azimuth;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist, double depthKm) {
        double[] out =  new double[2];
        Location evtLoc = evtLatLon.asLocation();
        LatLonSimple sta = distCalc.locForAzimuthDeg(evtLoc, azimuth, calcDist, depthKm);
        Location staLoc = sta.asLocation();
        return new double[] {staLoc.getLatitude(), staLoc.getLongitude(), staLoc.getDepthKm()};
    }
}
