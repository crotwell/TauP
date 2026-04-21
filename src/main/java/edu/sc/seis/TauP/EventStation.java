package edu.sc.seis.TauP;


import edu.sc.seis.TauP.cmdline.args.GeodeticArgs;
import edu.sc.seis.seisFile.LatLonLocatable;
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

    public EventStation(LatLonLocatable evt, LatLonLocatable sta, GeoDistType geoDistType, Geodesic geodesic) {
        super(geoDistType, geodesic);
        this.evt = evt;
        this.sta = sta;
    }

    @Override
    public double[] calcLatLon(double calcDist, double totalDist, double pointDepthKm) {
        double[] out =  new double[2];
        Location evtLoc = evt.asLocation();
        Location staLoc = sta.asLocation();
        if (isGeodetic()) {
            GeodesicLine gLine = geodesic.InverseLine(evtLoc.getLatitude(), evtLoc.getLongitude(),
                    staLoc.getLatitude(), staLoc.getLongitude());
            double km = calcDist*DtoR* DistAzKarney.averageRadiusKm(geodesic);
            GeodesicData gd = gLine.Position(km*1000);
            out[0] = gd.lat2;
            out[1] = gd.lon2;
        } else if (isGeocentric()) {
            //how to calc via geocentric???
            Geocentric gc = new Geocentric(geodesic);
            double hkm = -pointDepthKm;
            double azimuth = gc.azimuth(evtLoc.getLatitude(), evtLoc.getLongitude(), evtLoc.getDepthMeter(),
                    staLoc.getLatitude(), staLoc.getLongitude(), staLoc.getDepthMeter());
            out = gc.latLonForAzimuth(evtLoc.getLatitude(), evtLoc.getLongitude(), evtLoc.getDepthMeter(),
                    azimuth, calcDist, hkm*1000 );
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
