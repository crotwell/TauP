package edu.sc.seis.TauP;


import edu.sc.seis.TauP.cmdline.args.GeodeticArgs;
import edu.sc.seis.seisFile.LatLonLocatable;
import edu.sc.seis.seisFile.LatLonSimple;
import edu.sc.seis.seisFile.Location;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;

import java.util.List;

import static edu.sc.seis.TauP.SphericalCoords.DtoR;

/**
 * Calculatable ray from a source lat,lon to a receiver lat,lon.
 */
public class EventStation extends LatLonable {

    public EventStation(LatLonLocatable evt, LatLonLocatable sta, DistanceCalc distCalc) {
        super(distCalc);
        this.evt = evt;
        this.sta = sta;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist, double pointDepthKm) {
        Location evtLoc = evt.asLocation();
        Location staLoc = sta.asLocation();
        double azimuth = distCalc.azimuth(evtLoc, staLoc);
        Location pt = distCalc.locForAzimuthDeg(evtLoc, azimuth, calcDist, pointDepthKm).asLocation();
        return new double[] {pt.getLatitude(), pt.getLongitude(), pt.getDepthKm()};
    }

    LatLonLocatable evt;
    LatLonLocatable sta;
}
